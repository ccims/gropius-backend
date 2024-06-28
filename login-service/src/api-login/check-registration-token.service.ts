import {
    Injectable,
    Logger,
    UnauthorizedException,
} from "@nestjs/common";
import { TokenService } from "src/backend-services/token.service";
import { ActiveLogin } from "src/model/postgres/ActiveLogin.entity";
import { LoginUser } from "src/model/postgres/LoginUser.entity";
import { LoginState, UserLoginData } from "src/model/postgres/UserLoginData.entity";
import { ActiveLoginService } from "src/model/services/active-login.service";
import { UserLoginDataService } from "src/model/services/user-login-data.service";

/**
 * Service to validate a registration token and retrieve the referenced nodes
 */
@Injectable()
export class CheckRegistrationTokenService {
    private readonly logger = new Logger(CheckRegistrationTokenService.name);
    constructor(
        private readonly tokenService: TokenService,
        private readonly loginDataService: UserLoginDataService,
        private readonly activeLoginService: ActiveLoginService,
    ) {}

    /**
     * Checks wether both active login and login data are given and valid.
     *
     * Valid means not set invalid and not expired.
     * For the loginData it is checked, that it is still in the {@link LoginState.WAITING_FOR_REGISTER}
     *
     * @param activeLogin The activeLog innstance to check. Optional
     * @param loginData The loginData instance to check. Optional
     * @param activeLoginId The id of the activeLogin instance. Just used for error messages. Required
     * @throws {@link UnauthorizedException} If either activeLogin or loginData are not given or invalid in any way.
     */
    private checkActiveLoginValid(
        activeLogin: ActiveLogin | undefined | null,
        loginData: UserLoginData | undefined | null,
        activeLoginId: string,
    ) {
        if (!activeLogin) {
            this.logger.warn(`No active login with id from token; id:`, activeLoginId);
            throw new UnauthorizedException(undefined, "Register token is (no longer) valid");
        }
        if (!loginData) {
            this.logger.warn(`No login data for active login; id:`, activeLoginId);
            throw new UnauthorizedException(undefined, "Register token is (no longer) valid");
        }
        if (loginData.expires != null && loginData.expires <= new Date()) {
            throw new UnauthorizedException(undefined, "Login has expired; Registration did not happen in time");
        }
        if (activeLogin.expires != null && activeLogin.expires <= new Date()) {
            throw new UnauthorizedException(undefined, "Login has expired; Registration did not happen in time");
        }
        if (!activeLogin.isValid) {
            throw new UnauthorizedException(undefined, "Login has been set invalid");
        }
        if (loginData.state !== LoginState.WAITING_FOR_REGISTER) {
            this.logger.warn("State is not waiting for register of login data", loginData.id);
            throw new UnauthorizedException(undefined, "A user is already registered for this login");
        }
    }

    /**
     * Verifies that the loginData is for the exact user specified.
     *
     * Cases:
     * - If the loginData has no user, the check passes.
     * - If user is not given/undefined, the login data must be unassigned and have no user.
     * - If a user is given, the user of the loginData must be the same as the one given.
     *
     * @param loginData The loginData for which to verify the user
     * @param userMustBe The user to verify or undefined
     * @throws {@link UnauthorizedException} If the user did not match as specified above.
     */
    async verifyUserMatches(loginData: UserLoginData, userMustBe?: LoginUser | undefined) {
        const loginDataUser = await loginData.user;
        if (!!loginDataUser) {
            if (userMustBe) {
                if (loginDataUser.id !== userMustBe.id) {
                    this.logger.warn(
                        "User already exists and is not the user that currently tries to link for login data, user",
                        loginData.id,
                        userMustBe.id,
                    );
                    throw new UnauthorizedException(
                        undefined,
                        "This account is already registered and linked to a Gropius account",
                    );
                } else {
                    // ok. required user matches login data user
                }
            } else {
                this.logger.warn("User already esists for login data", loginData.id);
                throw new UnauthorizedException(undefined, "A user is already registered for this login");
            }
        }
    }

    /**
     * Validates a registration token and returns the loginData as well as activeLogin that created it.
     *
     * Additionally checks that the user linked to the loginData is the same as a given user.
     * This can be used to check that new registrations are only linked with oneself and not other user accounts
     *
     * @param token The token string to be validated
     * @returns The loginData and activeLogin objects that created the given registration token
     * @throws {@link UnauthorizedException} If no token is given, the token is invalid,
     * the referenced activeLogin or loginData don't exist or are invalid or the user does not match
     */
    async getActiveLoginAndLoginDataForToken(
        token: string | undefined | null,
        userMustBe?: LoginUser | undefined,
    ): Promise<{ loginData: UserLoginData; activeLogin: ActiveLogin }> {
        if (!token) {
            throw new UnauthorizedException(undefined, "No registration token given");
        }
        let activeLoginId: string;
        try {
            activeLoginId = await this.tokenService.verifyRegistrationToken(token);
        } catch (err) {
            this.logger.warn("Invalid registration token: ", err);
            throw new UnauthorizedException(undefined, "Invalid registration token: " + (err.message ?? err));
        }
        const activeLogin = await this.activeLoginService.findOneBy({
            id: activeLoginId,
        });
        const loginData = await activeLogin?.loginInstanceFor;
        this.checkActiveLoginValid(activeLogin, loginData, activeLoginId);

        await this.verifyUserMatches(loginData, userMustBe);

        return { loginData, activeLogin };
    }
}

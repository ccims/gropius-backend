import { Injectable, Logger } from "@nestjs/common";
import { GraphqlService } from "src/model/graphql/graphql.service";
import { StrategyInstance } from "src/model/postgres/StrategyInstance.entity";
import { UserLoginData } from "src/model/postgres/UserLoginData.entity";
import { UserLoginDataImsUser } from "src/model/postgres/UserLoginDataImsUser.entity";
import { UserLoginDataImsUserService } from "src/model/services/user-login-data-ims-user";
import { UserLoginDataService } from "src/model/services/user-login-data.service";
import { StrategiesService } from "src/model/services/strategies.service";
import { Strategy } from "src/strategies/Strategy";
import { jsonFieldArrayToObject, objectToJsonFieldArray } from "./JSONField";
import { BackendUserService } from "./backend-user.service";
import { deepEqual } from "fast-equals";

/**
 * Fields that are not compared on the templated fields of the ims bust instead on the fields of the ims directly
 */
const imsTemplatedDirectFields = ["id", "name", "description"];

/**
 * Fields that are not compared on the templated fields of the IMSUser,
 * but instead on the fields of the IMSUser directly
 */
const userTemplatedDirectFields = ["id", "username", "displayName", "email"];

@Injectable()
export class ImsUserFindingService {
    private readonly logger = new Logger(ImsUserFindingService.name);
    constructor(
        private readonly graphqlService: GraphqlService,
        private readonly strategiesService: StrategiesService,
        private readonly loginDataService: UserLoginDataService,
        private readonly imsUserService: UserLoginDataImsUserService,
        private readonly backendUserService: BackendUserService,
    ) {}

    /**
     * Throws an error with the following message:
     * Text for when an ims user entity is found in the database,
     * which has a different login data than the login data for which the ims user was found
     * using matching of templated values on ims and ims users
     *
     * @param imsUserNeo4jId The neo4j id of the ims user that was found to have a conflice
     * @param loginDataExpectedId The database id of the login data that is currently being processed
     * and for which the given ims user was found
     * @param loginDataActualId The database id of the login data that is already associated with the given ims user
     */
    private generateImsUserWithDifferentLoginDataError(
        imsUserNeo4jId: string,
        loginDataExpectedId: string,
        loginDataActualId: string,
    ): string {
        throw new Error(
            `The filtered resulting ims users included at least one ims user ` +
                `that is already assigned to another login data.\n ` +
                `This very likely means the filters of strategy instances overlap ` +
                `or the filters for users are not properly defined.\n ` +
                `IMSUser id with conflict: ${imsUserNeo4jId}` +
                `, current login data: ${loginDataExpectedId}` +
                `, conflicting loginData: ${loginDataActualId}`,
        );
    }

    /**
     * Checks if the values of the specified fields on the templatedValues match the values of those fields on the node
     * Deletes the keys that were compared from `templatedValue`
     *
     * Can be used directly on e.g. the ims as node to check the description
     * or on the templated fields of the ims to check those
     *
     * @param node The node object containing all values of keys to potentially check
     * @param requiredTemplatedValues The values that the node object must match to pass.
     * All fields to check will be deleted from this object
     * @param fieldsToCheck The names of the fields that should be checked for equivalence
     * on the node object directly instead of on its templated fields.
     * If not given, all keys of `templatedFields` will be matched
     * @returns `true` iff all specified keys that the templatedValues had a value fore matched, `false` else
     */
    private checkFieldsDirectly(node: object, requiredTemplatedValues: object, fieldsToCheck?: string[]): boolean {
        const fields = fieldsToCheck ? fieldsToCheck : Object.keys(requiredTemplatedValues);
        for (const key of fields) {
            if (requiredTemplatedValues[key]) {
                if (!node[key]) {
                    return false;
                }
                if (requiredTemplatedValues[key] != node[key]) {
                    return false;
                }
                if (deepEqual(requiredTemplatedValues[key], node[key])) {
                    return false;
                }
                delete requiredTemplatedValues[key];
            }
        }
        return true;
    }

    private async checkIfInstanceMatches(
        strategy: Strategy,
        instance: StrategyInstance,
        ims: object,
        imsTemplatedValues: { [key: string]: any },
    ): Promise<boolean> {
        const requiredTemplatedValues = await strategy.getImsTemplatedValuesForStrategyInstance(instance);
        if (requiredTemplatedValues == null) {
            this.logger.error(`Syncing strategy instance ${instance.id} did not provide required templated values`);
            return false;
        }
        if (!this.checkFieldsDirectly(ims, requiredTemplatedValues, ["id", "name", "description"])) {
            return false;
        }
        if (!this.checkFieldsDirectly(imsTemplatedValues, requiredTemplatedValues)) {
            return false;
        }
        return true;
    }

    private async getMatchingInstance(
        ims: object,
        imsTemplatedValues: { [key: string]: any },
    ): Promise<{ instance: StrategyInstance; strategy: Strategy }> {
        const allSyncStrategies = this.strategiesService.getAllStrategies().filter((s) => s.canSync);

        const matchingInstances: {
            instance: StrategyInstance;
            strategy: Strategy;
        }[] = [];

        await Promise.all(
            allSyncStrategies.map(async (strategy) => {
                const allInstances = await strategy.getAllInstances();
                await Promise.all(
                    allInstances.map(async (instance) => {
                        if (this.checkIfInstanceMatches(strategy, instance, ims, imsTemplatedValues)) {
                            matchingInstances.push({ instance, strategy });
                        }
                    }),
                );
            }),
        );

        if (matchingInstances.length != 1) {
            throw new Error(
                `Not exactly one (${matchingInstances.length}) instance matched the ims of the given ims user`,
            );
        }
        return matchingInstances[0];
    }

    /**
     * Extracts a subset of fields from the given object,
     * deletes them from the given object and returns them as seperate object
     *
     * @param data The object to extract fields from. This will be modified using `delete`
     * @param fields The names of the fields to remove from the object
     * @returns An object with the extracted fields
     */
    private extractFieldsFromObject(data: object, fields: string[]): { [field: string]: any } {
        const extracted = {};
        for (const key of fields) {
            extracted[key] = data[key];
            delete data[key];
        }
        return extracted;
    }

    /**
     * Converts an object of string keys and values to an object with the same keys
     * where the values are wrapped as a filter for graphql.
     *
     * Example:
     * - Input: `{test: "123", other: "key"}`
     * - Output: `{test: {eq: "123"}, other: {eq: "key"}}`
     *
     * @param data The object for which to wrap the values in (string) filters
     * @returns The filter object
     */
    private transformObjectToFilterObject(data: { [key: string]: string }): { [key: string]: { eq: string } } {
        const filterObject: { [key: string]: { eq: string } } = {};
        for (const key in data) {
            if (Object.prototype.hasOwnProperty.call(data, key)) {
                const value = data[key];
                filterObject[key] = { eq: value };
            }
        }
        return filterObject;
    }

    async getMatchingLoginData(
        matchingInstance: StrategyInstance,
        matchingStrategy: Strategy,
        imsUserTemplatedValues: object,
        imsUser: {
            id: string;
            username?: string;
            displayName: string;
            email?: string;
        },
    ): Promise<UserLoginData> {
        const requiredLoginDataData = matchingStrategy.getLoginDataDataForImsUserTemplatedFields({
            ...imsUserTemplatedValues,
            id: imsUser.id,
            username: imsUser.username,
            displayName: imsUser.displayName,
            email: imsUser.email,
        });
        if (!requiredLoginDataData) {
            throw new Error(
                "Strategy did not provide required login data data field for ims user. " +
                    "Make sure, the strategy can sync.",
            );
        }

        const matchingLoginData = await this.loginDataService.findForStrategyWithDataContaining(
            matchingInstance,
            requiredLoginDataData,
        );

        if (matchingLoginData.length != 1) {
            throw new Error(`Not exactly one (${matchingLoginData.length}) login data matched the ims user given`);
        }

        return matchingLoginData[0];
    }

    async findLoginDataForImsUser(imsUserId: string): Promise<UserLoginData> {
        const imsUserWithDetail = (await this.graphqlService.sdk.getImsUserDetails({ imsUserId })).node;
        if (imsUserWithDetail.__typename != "IMSUser") {
            throw new Error("id is not a ims user id");
        }

        const ims = imsUserWithDetail.ims;
        const imsTemplatedValues = jsonFieldArrayToObject(ims.templatedFields);
        const matchingInstance = await this.getMatchingInstance(ims, imsTemplatedValues);

        const imsUserTemplatedValues = jsonFieldArrayToObject(imsUserWithDetail["templatedFields"] ?? []);
        return this.getMatchingLoginData(
            matchingInstance.instance,
            matchingInstance.strategy,
            imsUserTemplatedValues,
            imsUserWithDetail,
        );
    }

    //todo: make more efficient (optimization by aggregating all imsusers created in one sync run)
    async createAndLinkSingleImsUser(imsUserId: string): Promise<UserLoginDataImsUser> {
        const existingImsUser = await this.imsUserService.findOneBy({
            neo4jId: imsUserId,
        });
        if (existingImsUser) {
            return existingImsUser;
        }
        const loginData = await this.findLoginDataForImsUser(imsUserId);

        let newImsUser = new UserLoginDataImsUser();
        newImsUser.neo4jId = imsUserId;
        newImsUser.loginData = Promise.resolve(loginData);
        newImsUser = await this.imsUserService.save(newImsUser);

        const loginUser = await loginData.user;
        if (loginUser) {
            await this.backendUserService.linkOneImsUserToGropiusUser(loginUser, newImsUser);
        }

        return newImsUser;
    }

    /**
     * Searches for IMSUsers that match the given login data as well as imss that don't have a single matching user
     *
     * Works by searching for IMSs of which the templated fields (and direct values) match the object given by
     * the strategy instance.
     * Then on these IMSs searches for IMSUsers of which the templated fields (and direct values) match the object
     * given by the login data.
     *
     * @param loginData The login data instance to search imsUsers and imss for
     * @returns The ims users that match the given logn data
     * and all imss that have no ims user matching the given login data
     */
    async findImsUserIdsForLoginData(
        loginData: UserLoginData,
    ): Promise<{ imsUserIds: string[]; imsIdsWithoutImsUsers: string[] }> {
        const strategyInstance = await loginData.strategyInstance;
        const strategy = this.strategiesService.getStrategyByName(strategyInstance.type);

        const requiredImsTemplatedValues = await strategy.getImsTemplatedValuesForStrategyInstance(strategyInstance);
        if (!requiredImsTemplatedValues) {
            throw new Error("Strategy instance didn't provide ims templated values. Check can sync");
        }
        const directRequiredIms = this.transformObjectToFilterObject(
            this.extractFieldsFromObject(requiredImsTemplatedValues, imsTemplatedDirectFields),
        );

        const requiredUserTemplatedFields = strategy.getImsUserTemplatedValuesForLoginData(loginData);
        if (!requiredUserTemplatedFields) {
            throw new Error("Strategy didn't provide required IMSUser templated values. Check can sync");
        }
        const directRequiredUser = this.transformObjectToFilterObject(
            this.extractFieldsFromObject(requiredUserTemplatedFields, userTemplatedDirectFields),
        );

        const matchingImsUsers = await this.graphqlService.sdk.getImsUsersByTemplatedFieldValues({
            imsFilterInput: {
                ...directRequiredIms,
                templatedFields: objectToJsonFieldArray(requiredImsTemplatedValues),
            },
            userFilterInput: {
                ...directRequiredUser,
                templatedFields: objectToJsonFieldArray(requiredUserTemplatedFields),
            },
        });

        this.logger.debug("Retrieved matching imss and ims users:", matchingImsUsers);
        return {
            imsUserIds: matchingImsUsers.imss.nodes.flatMap((ims) => ims.users.nodes.map((user) => user.id)),
            imsIdsWithoutImsUsers: matchingImsUsers.imss.nodes
                .filter((ims) => ims.users.nodes.length == 0)
                .map((ims) => ims.id),
        };
    }

    async createNewImsUserInBackendForLoginDataAndIms(loginData: UserLoginData, imsId: string): Promise<string> {
        const strategyInstance = await loginData.strategyInstance;
        const strategy = this.strategiesService.getStrategyByName(strategyInstance.type);
        const templatedFieldValuesForImsUser = strategy.getImsUserTemplatedValuesForLoginData(loginData);
        const directValues = this.extractFieldsFromObject(templatedFieldValuesForImsUser, userTemplatedDirectFields);
        const templatedValuesAsArray = objectToJsonFieldArray(templatedFieldValuesForImsUser);
        let findPossibleDisplayName = directValues["displayName"] ?? directValues["username"] ?? directValues["email"];
        if (!findPossibleDisplayName) {
            for (const key in templatedFieldValuesForImsUser) {
                if (Object.prototype.hasOwnProperty.call(templatedFieldValuesForImsUser, key)) {
                    const value = templatedFieldValuesForImsUser[key];
                    if (value) {
                        findPossibleDisplayName = value;
                        break;
                    }
                }
            }
        }
        const result = await this.graphqlService.sdk.createNewImsUserInIms({
            input: {
                ims: imsId,
                username: directValues["username"],
                displayName: findPossibleDisplayName ?? "Unknown username",
                email: directValues["email"],
                templatedFields: templatedValuesAsArray,
            },
        });
        return result.createIMSUser.imsUser.id;
    }

    /**
     * Loads the ims user entities from the database for a given set of backend ids of ims users.
     * Additionally fetches the id of the login data to which the entity belongs.
     *
     * For every given id returns the given id, the ims user entity (if existing)
     * and the login data id (if ims user entity exists).
     * If ims user doesn't exist, just returns the given neo4jid
     * @param imsUserIds The ist of neo4j/backend ids for which to fetch the ims user
     * @returns The list of the ims user entities loaded for the given neo4j id and the id of the login data
     */
    private async getKnownImsUsersForIds(
        imsUserIds: string[],
    ): Promise<{ id: string; imsUser: UserLoginDataImsUser; loginDataId: string }[]> {
        return Promise.all(
            imsUserIds.map(async (id) => {
                const imsUser = await this.imsUserService.findOneBy({
                    neo4jId: id,
                });
                const loginDataId = (await imsUser?.loginData)?.id ?? null;
                return { id, imsUser, loginDataId };
            }),
        );
    }

    /**
     * Creates and saves ims user entities for the given neo4jids
     * and links them to the given login data object.
     *
     * Caller must make sure, that no ims user entity for the given neo4j ids exist yet.
     *
     * @param neo4jImsUserIds The list of neo4j ids for which to create a ims user in the database
     * @param loginData The login data to link with the newly created ims user
     * @returns The list of all the ims user entites crated
     */
    private async createImsUserEntitiesForNeo4jIds(
        neo4jImsUserIds: string[],
        loginData: UserLoginData,
    ): Promise<UserLoginDataImsUser[]> {
        return Promise.all(
            neo4jImsUserIds.map((id) => {
                const user = new UserLoginDataImsUser();
                user.neo4jId = id;
                user.loginData = Promise.resolve(loginData);
                return this.imsUserService.save(user);
            }),
        );
    }

    /**
     * Performs IMSUser search creation and linkage
     *
     * Steps:
     * - Searches all IMSUser instances in the backend that match the given login data.
     * - For IMSs which don't have a single matching IMSUser, creates new IMSUsers in the backend.
     * - For all found and created IMSUsers, finds existing ims user entities in the database
     * - If there is an ims user entity with a **different** login data, fails. Filters are to unprecise
     * - Creates new ims user entities for all found and created IMSUsers that don't have an entity yet
     * - If the login data has a user linked:
     *     For all created ims user entities links the corresponding IMSUser and GropiusUser
     *
     * @param loginData The login data for which to perform ims user search and linking
     * @returns A list of all ims user entitites created as result of the search (not including existing entities)
     */
    async createAndLinkImsUsersForLoginData(loginData: UserLoginData): Promise<UserLoginDataImsUser[]> {
        const { imsUserIds, imsIdsWithoutImsUsers } = await this.findImsUserIdsForLoginData(loginData);

        const listOfKnownImsUsers = await this.getKnownImsUsersForIds(imsUserIds);
        const imsUserWithDifferentLoginData = listOfKnownImsUsers.find(
            (result) => (result.loginDataId != null || result.imsUser != null) && result.loginDataId != loginData.id,
        );
        if (!!imsUserWithDifferentLoginData) {
            this.generateImsUserWithDifferentLoginDataError(
                imsUserWithDifferentLoginData.imsUser.neo4jId,
                loginData.id,
                imsUserWithDifferentLoginData.loginDataId,
            );
        }

        const imsUserIdsWithoutEntity = listOfKnownImsUsers.filter((user) => !user.imsUser).map((user) => user.id);
        const backendCreatedImsUserIds = await Promise.all(
            imsIdsWithoutImsUsers.map((ims) => this.createNewImsUserInBackendForLoginDataAndIms(loginData, ims)),
        );
        const newImsUsers = await this.createImsUserEntitiesForNeo4jIds(
            imsUserIdsWithoutEntity.concat(backendCreatedImsUserIds),
            loginData,
        );

        const loginUser = await loginData.user;
        if (loginUser) {
            await Promise.all(
                newImsUsers.map((imsUser) => this.backendUserService.linkOneImsUserToGropiusUser(loginUser, imsUser)),
            );
        }

        return newImsUsers;
    }
}

import {
    HttpException,
    HttpStatus,
    Injectable,
    NestMiddleware,
} from "@nestjs/common";
import { Request, Response } from "express";
import { StrategyInstance } from "src/model/postgres/StrategyInstance";
import { StrategyInstanceService } from "src/model/services/strategy-instance.service";
import { AuthFunction, AuthStateData } from "./AuthResult";
import { PerformAuthFunctionService } from "./perform-auth-function.service";
import { StrategiesService } from "./strategies.service";
import { Strategy } from "./Strategy";
import { ensureState } from "./utils";

@Injectable()
export class StrategiesMiddleware implements NestMiddleware {
    constructor(
        private readonly strategiesService: StrategiesService,
        private readonly strategyInstanceService: StrategyInstanceService,
        private readonly performAuthFunctionService: PerformAuthFunctionService,
    ) {}

    private async idToStrategyInstance(id: string): Promise<StrategyInstance> {
        if (!id) {
            throw new HttpException(
                "No Id of strategy instance given",
                HttpStatus.BAD_REQUEST,
            );
        }
        const instance = await this.strategyInstanceService.findOneBy({ id });
        if (!instance) {
            throw new HttpException(
                `No Strategy instance with id ${id}`,
                HttpStatus.NOT_FOUND,
            );
        }
        return instance;
    }

    async use(req: Request, res: Response, next: () => void) {
        ensureState(res);
        if ((res.locals.state as AuthStateData)?.authErrorMessage) {
            next();
        }
        const id = req.params.id;
        console.log("id", id);
        const instance = await this.idToStrategyInstance(id);
        const strategy = await this.strategiesService.getStrategyByName(
            instance.type,
        );
        console.log("strategies middleware; state", res.locals.state);

        const functionError =
            this.performAuthFunctionService.checkFunctionIsAllowed(
                res.locals.state,
                instance,
                strategy,
            );
        if (functionError != null) {
            (res.locals.state as AuthStateData).authErrorMessage =
                functionError;
            return next();
        }

        const result = await strategy.performAuth(
            instance,
            res.locals.state || {},
            req,
            res,
        );
        console.log("Strategy result", result);
        res.locals.state = { ...res.locals.state, ...result.returnedState };

        const authResult = result.result;
        if (authResult) {
            const executionResult =
                await this.performAuthFunctionService.performRequestedAction(
                    authResult,
                    res.locals.state,
                    instance,
                    strategy,
                );
            res.locals.state = { ...res.locals.state, ...executionResult };
        } else {
            (res.locals.state as AuthStateData).authErrorMessage =
                result.info?.message?.toString() ||
                JSON.stringify(result.info) ||
                "Login unsuccessfull";
        }
        next();
    }
}

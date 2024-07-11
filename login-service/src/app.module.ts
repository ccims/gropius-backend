import { DynamicModule, Module } from "@nestjs/common";
import { ConfigModule } from "@nestjs/config";
import { LazyModuleLoader, RouterModule } from "@nestjs/core";
import { TypeOrmModule } from "@nestjs/typeorm";
import { ApiLoginModule } from "./api-login/api-login.module";
import { ApiSyncModule } from "./api-sync/api-sync.module";
import { ModelModule } from "./model/model.module";
import { StrategiesModule } from "./strategies/strategies.module";
import { BackendServicesModule } from "./backend-services/backend-services.module";
import { validationSchema } from "./configuration-validator";
import { OauthServerModule } from "./oauth-server/oauth-server.module";
import { DefaultReturn } from "./default-return.dto";
import { InitializationModule } from "./initialization/initialization.module";
import * as path from "path";
import { ServeStaticModule } from "@nestjs/serve-static";

@Module({
    imports: [
        ConfigModule.forRoot({
            envFilePath: ["development", "testing"].includes(process.env.NODE_ENV)
                ? [".env.dev.local", ".env.dev"]
                : [".env.prod.local", ".env.prod"],
            validationSchema,
        }),
        TypeOrmModule.forRootAsync({
            async useFactory(...args) {
                await ConfigModule.envVariablesLoaded;
                const driver = process.env.GROPIUS_LOGIN_DATABASE_DRIVER;
                if (!driver || driver == "postgres") {
                    return {
                        type: "postgres",
                        host: process.env.GROPIUS_LOGIN_DATABASE_HOST,
                        port: parseInt(process.env.GROPIUS_LOGIN_DATABASE_PORT, 10),
                        username: process.env.GROPIUS_LOGIN_DATABASE_USER,
                        password: process.env.GROPIUS_LOGIN_DATABASE_PASSWORD,
                        database: process.env.GROPIUS_LOGIN_DATABASE_DATABASE,
                        synchronize: process.env.NODE_ENV === "development",
                        autoLoadEntities: true,
                        migrations: [path.join(__dirname, "..", "dist", "database-migrations", "*.js")],
                    };
                } else if (driver == "sqlite") {
                    return {
                        type: "sqlite",
                        database: process.env.GROPIUS_LOGIN_DATABASE_DATABASE + ".sqlite",
                    };
                } else {
                    return {};
                }
            },
        }),
        ServeStaticModule.forRoot({
            rootPath: path.join(__dirname, "..", "static"),
            renderPath: "a",
            exclude: ["/login", "/syncApi", "/authenticate"],
        }),
        ModelModule,
        ApiLoginModule,
        ApiSyncModule,
        StrategiesModule,
        OauthServerModule,
        RouterModule.register([
            { path: "login", module: ApiLoginModule },
            { path: "login", module: StrategiesModule },
            { path: "syncApi", module: ApiSyncModule },
            { path: "authenticate", module: OauthServerModule },
        ]),
        BackendServicesModule,
        InitializationModule,
    ],
    controllers: [],
    providers: [],
})
export class AppModule {}

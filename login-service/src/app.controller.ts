import { Controller, Get, Post } from "@nestjs/common";
import { UserDataFragment } from "./model/generated";
import { GraphqlService } from "./model/graphql/graphql.service";

@Controller()
export class AppController {
    @Get()
    async getHello(): Promise<string> {
        return "";
    }
}

import { Injectable, Optional } from "@nestjs/common";
import { getSdk } from "./generated";
import { GraphQLClient } from "graphql-request";

@Injectable()
export class GraphqlService {
    private readonly client: GraphQLClient;
    public readonly sdk: ReturnType<typeof getSdk>;

    constructor(
        @Optional()
        internalApiEndpoint = process.env.GROPIUS_INTERNAL_BACKEND_ENDPOINT,
        @Optional()
        internalApiToken = process.env.GROPIUS_INTERNAL_BACKEND_TOKEN,
    ) {
        this.client = new GraphQLClient(internalApiEndpoint, {
            headers: {
                Authorization: internalApiToken ? "Bearer " + internalApiToken : undefined,
            },
        });
        this.sdk = getSdk(this.client);
    }
}

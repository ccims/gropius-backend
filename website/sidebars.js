/**
 * Helper to load the api sidebar JSON only if it exists
 * returns empty array otherwise
 */
function loadApiSidebarIfExists() {
    try {
        return require("./docs/api/sidebar.json")
    } catch {
        return []
    }
}

/** @type {import("@docusaurus/plugin-content-docs").SidebarsConfig} */
const sidebars = {
    docSidebar: [{ type: "autogenerated", dirName: "docs" }],
    apiSidebar: loadApiSidebarIfExists(),
    graphqlSidebar: [
        {
            type: "category",
            label: "Public API",
            link: {
                type: "doc",
                id: "graphql/api-public"
            },
            items: [{
                type: "autogenerated",
                dirName: "graphql/api-public"
            }]
        },
        {
            type: "category",
            label: "Internal API",
            link: {
                type: "doc",
                id: "graphql/api-internal"
            },
            items: [{
                type: "autogenerated",
                dirName: "graphql/api-internal"
            }]
        }
    ]
};

module.exports = sidebars;
# Robot-car issue icons

Optional icon assets for an iPraktikum Gropius issue template. These are project configuration examples; the adapter does not change issue icons automatically.

| Issue type | Icon | Source |
| --- | --- | --- |
| Bug report | ![Bug](bug-report-24.svg) | `bug.svg` |
| Feature request | ![Feature](feature-request-24.svg) | `feature.svg` |
| Task | ![Task](task-24.svg) | `uncategorized.svg` |
| Documentation | ![Documentation](documentation-24.svg) | Custom document outline |

The first three shapes are adapted from [ccims/gropius-icons, icons-md/normal](https://github.com/ccims/gropius-icons/tree/main/icons-md/normal). The documentation icon follows the same line weight, with a folded corner and text lines. Original 32×32 geometry is scaled to 24×24, and stroked primitives are expanded to filled paths with opposite inner contours. Gropius supplies the icon color and relation/state decorations.

`issue-type-paths.json` maps example issue-type names to the single SVG path expected by Gropius. In the Gropius template editor, choose the relevant type and paste its path into the custom icon picker. For scripted setup, use each path as `iconPath` in the issue template's `issueTypes` entries, or update an existing type using its runtime ID:

```graphql
mutation UpdateIssueIcon($id: ID!, $iconPath: String!) {
  updateIssueType(input: { id: $id, iconPath: $iconPath }) {
    issueType { id name iconPath }
  }
}
```

Read back `iconPath` to verify the update. It applies to all issues of that type. GitLab retains its native issue icons. These assets do not require a running sync adapter.

The upstream shapes are MIT licensed; see [GROPIUS-ICONS-LICENSE](GROPIUS-ICONS-LICENSE). The custom documentation icon is covered by this repository's license.

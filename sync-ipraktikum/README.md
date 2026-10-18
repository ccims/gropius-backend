# iPraktikum issue synchronization

Dedicated adapter for one self-hosted GitLab Free monorepo and one Gropius cohort project. Components and template vocabulary are discovered at runtime. All in-scope issues are enrolled automatically; participants share visibility. GitHub/Jira workers must not run against this cohort.

## Start the deployment

In the deployment repository, use either:

```sh
docker compose -f docker-compose.yaml -f docker-compose-ipraktikum.yaml up --build -d
# Development setup instead:
docker compose -f docker-compose-testing.yaml -f docker-compose-ipraktikum-testing.yaml up --build -d
```

Before starting, set `GROPIUS_GITLAB_TOKEN_ENCRYPTION_KEY` to a persistent base64-encoded 32-byte random key, e.g. generate it with `openssl rand -base64 32`. Keep it in the deployment's secret configuration and backups. Changing or losing it prevents decryption of stored GitLab tokens. The normal production Neo4j/MongoDB/sync secrets remain required. The overlays disable the traditional sync workers through a separate profile.

The worker starts with no enrolled repositories. It creates the **iPraktikum-Issue-Sync** IMS template. You can create components and issue templates before or after the worker starts.

## Connect GitLab and configure the cohort

1. Create an IMS with the iPraktikum template. Set `gitlab-url` to the instance base URL (including any installation path, without `/api/v4` or a trailing slash).
2. In the login-service strategy administration, create a **gitlab-token** strategy instance with:

   ```json
   { "imsTemplatedFieldsFilter": { "gitlab-url": "https://gitlab.example.edu" } }
   ```

3. From the Gropius bot account's account/authentication screen, link that strategy with the bot's PAT. The token needs API access and the project permissions for issues, labels, notes, and links. The strategy verifies `/api/v4/user`, matches by instance plus numeric user ID, and stores the PAT with authenticated encryption. GitLab OAuth sign-in is not required.
4. Select the resulting IMSUser as the IMS `read-user`. Allow that account to **sync others** on the IMS or binding, using the existing sync consent controls.
5. Create one IMSProject under that IMS, attached to the Gropius cohort **Project**, and set its numeric `gitlab-project-id`.
6. Open that IMSProject's General page. The iPraktikum panel shows the runtime catalog and worker status. Refresh discovery after creating templates/components. Choose the incoming default template, enable the templates used by the cohort, and select their issue type and open/closed/reopen defaults. Configure required custom-field defaults under `requiredFields` in the advanced profile if needed.
7. Leave **Dry run** enabled initially and enable synchronization. Inspect discovery/status. Turn dry run off when ready for the first import/export. Dry run updates discovery/status and performs reads but makes no issue/label/comment mutations.

The normal IMSProject permission checks protect profile updates. Status/catalog are refreshed by the worker; do not edit them as configuration. The default polling interval is one minute (`GROPIUS_IPRAKTIKUM_INTERVAL` in milliseconds); `GROPIUS_IPRAKTIKUM_MAX_WRITES` limits each binding's writes per cycle, default 100. Work beyond the budget remains pending.

## Mapping and everyday use

* Component labels default to `component/<stable-key>`. Use `componentLabels` in the advanced profile to give them readable aliases, e.g. `{ "frontend-<stable-suffix>": "frontend" }` produces `component/frontend`. The discovered stable key remains the internal identity. Aliases must be unique lowercase slugs; two components with the same name need distinct aliases. Several labels map the **same issue** to several Gropius components. Unclassified issues remain on the cohort project.
* Discovery follows `Project → ComponentVersion → Component`. Adding another version of an enrolled component does not create another component label. Renames retain existing keys; excluded/removed components are not deleted from issue history.
* State labels are ordinary `workflow/<key>` labels. Create GitLab board label lists for the desired workflow buckets. Gropius boards can group their finer states independently. Closed states close the GitLab issue, which appears in its built-in Closed list.
* With `autoStates: true`, each discovered state has its own workflow label. To use a coarser GitLab board, configure `states` as `{ "exact-state-key": "bucket-key" }` and `bucketDefaults` as `{ "bucket-key": "default-exact-state-key" }`. A bucket cannot mix open/closed states. Unchanged coarse labels preserve the exact state. A move to another bucket chooses its configured default.
* Ordinary label assignments synchronize in both directions, including removals and labels already present when upgrading. Label names match exactly within the shared cohort; use one label definition per name. New counterpart labels copy the source color and description, while existing definitions retain their attributes. Component-scoped labels are shared with the cohort project when needed on another component. Unused label definitions are retained; renames behave as removal of the old name and addition of the new one. The prefixes `component/`, `workflow/`, `type/`, and `priority/` are reserved for adapter projections. Names containing commas, surrounding whitespace, control characters, or more than 255 characters pause synchronization rather than being rewritten.
* Choose a role for the optional native-assignee projection. Other assignments remain in the editable description. `primary_assignment` may be an assignment ID, `"auto"`, or null (intentionally unassigned). There is no hard-coded implementer/reviewer vocabulary.
* Choose a genuinely symmetric relation type for optional native generic links. Other relations retain their direction/type in metadata. Outgoing relations are edited on their source issue. Relation targets use `#<iid>` within this monorepo, or `sync:<identity>` while an endpoint awaits export. External targets are retained in Gropius and are not exported automatically.
* Use the generated **iPraktikum** description section for exact state/template/type, multiple assignment roles, and typed relations. The JSON initially rendered inside the YAML fence is valid YAML; students may edit it using ordinary YAML syntax. The runtime catalog in the setup panel supplies valid keys. Keep the identity marker and delimiters intact. Remove an entry or use an empty list to remove semantics intentionally; deleting/corrupting the entire section pauses that issue instead of clearing its data.
* Comments retain identity and attribution. Imported human-owned notes receive linked corrections when edited in Gropius; the bot does not impersonate users. Deletes retain the counterpart and a local tombstone. System/internal/control notes are excluded from normal comment import.

## Runtime profile reference

The `sync-profile` field is a JSON-encoded string in the IMSProject's templated fields; the setup panel handles this encoding. Keys and choices come from the discovered catalog.

```json
{
  "enabled": true,
  "dryRun": true,
  "revision": 1,
  "defaultTemplate": "<template-key>",
  "templates": {
    "<template-key>": {
      "defaultType": "<type-key>",
      "initialState": "<open-state-key>",
      "closedState": "<closed-state-key>",
      "reopenState": "<open-state-key>",
      "autoStates": true,
      "states": {},
      "bucketDefaults": {},
      "requiredFields": {},
      "nativeAssignmentEnabled": false,
      "nativeAssignmentRole": null,
      "genericRelationEnabled": false,
      "genericRelationType": null
    }
  },
  "excludedComponents": [],
  "componentLabels": {},
  "primary": {},
  "resolutions": {},
  "detached": [],
  "adoptCopies": [],
  "retryCreates": {},
  "repairLabels": []
}
```

`componentLabels` maps discovered component keys to GitLab label suffixes (without `component/`). Changing a profile migrates issue label assignments to the configured alias. Legacy stable-key labels remain readable during migration; labels no longer used by any issue may remain in the GitLab project. Existing GitLab labels with the alias name are reused. Renaming a managed label directly in GitLab still requires an administrator-controlled catalog repair; set the alias in the profile first.

`primary` maps sync issue IDs to an assignment ID, `"auto"`, or null. `resolutions` maps a sync ID to `{ "fingerprint": "<reported-conflict-hash>", "side": "gropius" }` or `"gitlab"`; an intervening edit invalidates that fingerprint. `detached` contains issue sync IDs to stop syncing permanently. `adoptCopies` contains GitLab **IIDs** explicitly approved as new issues after copying an existing identity marker. These operations are binding-scoped; issue identities are never inferred from titles or names.

`repairLabels` contains the full names of adapter labels explicitly approved for recreation/rebinding after catalog deletion or renaming. `retryCreates` maps a sync ID to the recovery token shown in its error, after an administrator verifies that the prior create did not succeed. The token changes if a retry also has an unknown outcome. Remove completed repair/retry entries after recovery.

## Recovery and limits

The canonical integration records, local mutation receipts, mappings, leases, and outbox are stored under Neo4j's `IPraktikumRecord` label. Back up these alongside domain data. MongoDB stores fetched GitLab snapshots in `ipraktikum_inbox`; complete scans also reconcile child notes and links, so changes are not dependent on an issue timestamp or webhook delivery. Initial implementation uses complete repository scans rather than incremental polling; this is intended for cohort-sized repositories.

If a create request times out, the worker searches for its exact identity marker. If it cannot establish the outcome, the issue remains paused rather than creating a duplicate. An administrator must verify absence before authorizing a retry with the reported recovery token. Do not delete the identity registry to clear an error. Existing missing/removed issues are not recreated or deleted automatically. Label catalog deletion/renaming is surfaced as an error, separate from deliberately unlabeling an issue.

Best-effort concurrent editing is the agreed contract. The worker refetches before writes and verifies observed results, but a description replacement can still overwrite an intervening edit. Students should avoid editing the same issue simultaneously. Conflicting semantic edits pause an issue for resolution; other issues continue. Configure all templates used by the binding before enabling writes. Arbitrary required custom fields, attachment transfer, architectural `affects` mapping, and full historical authorship reproduction are not inferred.

## Automated checks and collaborative testing

```sh
./gradlew :sync-ipraktikum:test :sync-ipraktikum:bootJar
# Isolated real Neo4j/MongoDB + stateful GitLab HTTP fixture:
docker compose -p ipraktikum-adapter-tests -f sync-ipraktikum/compose.test.yaml up -d --wait
IPRAKTIKUM_INTEGRATION=true ./gradlew :sync-ipraktikum:test --rerun-tasks
docker compose -p ipraktikum-adapter-tests -f sync-ipraktikum/compose.test.yaml down
```

From login-service `backend/`: `npm ci && npm run build && node --test test/gitlab-token.test.mjs`.
From frontend: `npm ci && npm run build --workspace @gropius/graph-editor && npx vue-tsc --noEmit && npm run build`.

The fixture verifies the actual Gropius domain transactions, metadata protocol, merge rules, HTTP client, enrollment, and repeated-cycle behavior. It is not a substitute for testing against the target GitLab version/permissions. For collaborative testing, start with an empty disposable monorepo and cohort, perform a dry run, then exercise issue creation from both sides, multi-component routing, coarse/fine states and boards, multiple roles, directed relations, comments, worker restart, and copied/malformed metadata.

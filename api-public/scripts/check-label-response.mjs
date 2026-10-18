/** Real API regression check. Use a disposable issue and an existing, initially unassigned label.
 * The check creates timeline entries; label membership is restored after every iteration.
 * GROPIUS_TEST_ACCESS_TOKEN=<token> node check-label-response.mjs <graphql-url> <issue-id> <label-id> [iterations]
 */
import { pathToFileURL } from 'node:url';

export async function checkLabelResponses({ endpoint, accessToken, issue, label, iterations = 40 }) {
    async function execute(query) {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${accessToken}` },
            body: JSON.stringify({ query, variables: { issue, label } })
        });
        if (!response.ok) throw new Error(`GraphQL HTTP ${response.status}`);
        return response.json();
    }
    const state = 'query($issue:ID!){node(id:$issue){...on Issue{labels{nodes{id}}}}}';
    const initial = await execute(state);
    if (initial.errors || !initial.data?.node?.labels) throw new Error('Cannot read the test issue');
    if (initial.data.node.labels.nodes.some(item => item.id === label)) throw new Error('Choose an initially unassigned label');
    const add = `mutation($issue:ID!,$label:ID!){
        addLabelToIssue(input:{issue:$issue,label:$label}){
            addedLabelEvent{id createdAt createdBy{id username displayName avatar} addedLabel{id name description color}}
        }
    }`;
    const remove = 'mutation($issue:ID!,$label:ID!){removeLabelFromIssue(input:{issue:$issue,label:$label}){removedLabelEvent{id}}}';
    const errors = [];
    for (let iteration = 0; iteration < iterations; iteration++) {
        try {
            const result = await execute(add);
            const event = result.data?.addLabelToIssue?.addedLabelEvent;
            if (result.errors) errors.push({ iteration, messages: result.errors.map(item => item.message) });
            else if (event?.addedLabel?.id !== label || !event.createdBy?.id) errors.push({ iteration, messages: ['Incomplete mutation response'] });
        } finally {
            const cleaned = await execute(remove);
            if (cleaned.errors) throw new Error('Could not restore label membership: ' + JSON.stringify(cleaned.errors));
        }
    }
    const final = await execute(state);
    if (final.errors || final.data.node.labels.nodes.some(item => item.id === label)) throw new Error('Label membership was not restored');
    return { iterations, failures: errors.length, errors };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    const [endpoint, issue, label, count = '40'] = process.argv.slice(2);
    const accessToken = process.env.GROPIUS_TEST_ACCESS_TOKEN;
    if (!endpoint || !issue || !label || !accessToken || !Number.isInteger(Number(count)) || Number(count) <= 0) {
        throw new Error('Provide endpoint, disposable issue ID, label ID, positive iteration count, and GROPIUS_TEST_ACCESS_TOKEN');
    }
    const result = await checkLabelResponses({ endpoint, accessToken, issue, label, iterations: Number(count) });
    console.log(JSON.stringify(result, null, 2));
    if (result.failures) process.exitCode = 1;
}

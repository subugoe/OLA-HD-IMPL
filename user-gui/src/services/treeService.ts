export default {

    /**
     * Get a node from a given path.
     * @param tree The tree to traverse.
     * @param path Path to the node on the tree.
     */
    getNode(tree: Array<{id: string, label: string, name: string, children: any}>, path: string) {

        // Break the path into parts
        let parts = path.split('/');

        // Select that node in the tree
        let node;
        let currentLevel = tree;

        // For each part in the path
        for (let part of parts) {

            // Check all node in current level
            for (let item of currentLevel) {

                // Found the node that match the current part
                if (item.name === part) {
                    node = item;

                    // Not a leaf node? Go deeper
                    if (item['children']) {
                        currentLevel = item.children;
                    }
                    break;
                }
            }
        }

        return node;
    },

    /**
     * Using Breadth First Search to find all leaf nodes under the input node.
     * @param inputNode The root, where the search begins.
     */
    getLeafNodes(inputNode: {id: string, label: string, name: string, children: any}) {

        // Output
        let leafNodes = [];

        // Init the queue
        let queue = [];
        queue.push(inputNode);

        // Loop through the queue
        while (queue.length > 0) {

            // Take the first node out
            let currentNode: any = queue.pop();

            // If it's a leaf node
            if (!currentNode['children']) {
                leafNodes.push(currentNode);
                continue;
            }

            // If it's not a leaf node, add all its children to the queue
            for (let child of currentNode.children) {
                queue.push(child);
            }
        }

        return leafNodes;
    }
}
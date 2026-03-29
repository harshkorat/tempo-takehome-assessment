import kotlin.test.*

/**
 * A `Hierarchy` stores an arbitrary _forest_ (an ordered collection of ordered trees)
 * as an array of node IDs in the order of DFS traversal, combined with a parallel array of node depths.
 *
 * Parent-child relationships are identified by the position in the array and the associated depth.
 * Each tree root has depth 0, its children have depth 1 and follow it in the array, their children have depth 2 and follow them, etc.
 *
 * Example:
 * ```
 * nodeIds: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
 * depths:  0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2
 * ```
 *
 * the forest can be visualized as follows:
 * ```
 * 1
 * - 2
 * - - 3
 * - - - 4
 * - 5
 * 6
 * - 7
 * 8
 * - 9
 * - 10
 * - - 11
 *```
 */
interface Hierarchy {
    /** The number of nodes in the hierarchy. */
    val size: Int

    /**
     * Returns the unique ID of the node identified by the hierarchy index.
     * @param index must be non-negative and less than [size]
     */
    fun nodeId(index: Int): Int

    /**
     * Returns the depth of the node identified by the hierarchy index.
     * @param index must be non-negative and less than [size]
     */
    fun depth(index: Int): Int

    fun formatString(): String {
        return (0 until size).joinToString(
            separator = ", ",
            prefix = "[",
            postfix = "]"
        ) { i -> "${nodeId(i)}:${depth(i)}" }
    }
}

/**
 * A node is present in the filtered hierarchy iff its node ID passes the predicate
 * and all of its ancestors pass it as well.
 */
fun Hierarchy.filter(nodeIdPredicate: (Int) -> Boolean): Hierarchy {
    if (size == 0) return ArrayBasedHierarchy(IntArray(0), IntArray(0))

    val filteredNodeIds = mutableListOf<Int>()
    val filteredDepths = mutableListOf<Int>()

    // Tracks whether the node at each depth is included
    val includedAtDepth = mutableListOf<Boolean>()

    for (i in 0 until size) {
        val currentNodeId = nodeId(i)
        val currentDepth = depth(i)

        // Ensure includedAtDepth size matches current traversal depth
        while (includedAtDepth.size > currentDepth) {
            includedAtDepth.removeAt(includedAtDepth.lastIndex)
        }

        val parentIncluded = if (currentDepth == 0) true else includedAtDepth[currentDepth - 1]
        val currentIncluded = parentIncluded && nodeIdPredicate(currentNodeId)

        if (currentIncluded) {
            filteredNodeIds.add(currentNodeId)
            filteredDepths.add(currentDepth)
        }

        includedAtDepth.add(currentIncluded)
    }

    return ArrayBasedHierarchy(
        filteredNodeIds.toIntArray(),
        filteredDepths.toIntArray()
    )
}

class ArrayBasedHierarchy(
    private val myNodeIds: IntArray,
    private val myDepths: IntArray,
) : Hierarchy {
    override val size: Int = myDepths.size

    override fun nodeId(index: Int): Int = myNodeIds[index]

    override fun depth(index: Int): Int = myDepths[index]
}

class FilterTest {
    @Test
    fun testFilter() {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
            intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2)
        )

        val filteredActual: Hierarchy = unfiltered.filter { nodeId -> nodeId % 3 != 0 }

        val filteredExpected: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(1, 2, 5, 8, 10, 11),
            intArrayOf(0, 1, 1, 0, 1, 2)
        )

        assertEquals(filteredExpected.formatString(), filteredActual.formatString())
    }

    @Test
    fun testEmptyHierarchy() {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(intArrayOf(), intArrayOf())
        val filtered = unfiltered.filter { it % 2 == 0 }
        assertEquals("[]", filtered.formatString())
    }

    @Test
    fun testAllNodesPass() {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(1, 2, 3),
            intArrayOf(0, 1, 1)
        )

        val filtered = unfiltered.filter { true }

        assertEquals("[1:0, 2:1, 3:1]", filtered.formatString())
    }

    @Test
    fun testAllNodesFail() {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(1, 2, 3),
            intArrayOf(0, 1, 2)
        )

        val filtered = unfiltered.filter { false }

        assertEquals("[]", filtered.formatString())
    }

    @Test
    fun testRootFailsRemovesWholeSubtree() {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(1, 2, 3, 4),
            intArrayOf(0, 1, 2, 1)
        )

        val filtered = unfiltered.filter { it != 1 }

        assertEquals("[]", filtered.formatString())
    }

    @Test
    fun testChildFailsRemovesDescendantsOnly() {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(1, 2, 3, 4, 5),
            intArrayOf(0, 1, 2, 1, 2)
        )

        val filtered = unfiltered.filter { it != 2 }

        assertEquals("[1:0, 4:1, 5:2]", filtered.formatString())
    }

    @Test
    fun testMultipleRoots() {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(1, 2, 3, 4, 5),
            intArrayOf(0, 1, 0, 1, 0)
        )

        val filtered = unfiltered.filter { it != 3 }

        assertEquals("[1:0, 2:1, 5:0]", filtered.formatString())
    }

    @Test
    fun testLeafNodeFiltering() {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(1, 2, 3),
            intArrayOf(0, 1, 2)
        )

        val filtered = unfiltered.filter { it != 3 }

        assertEquals("[1:0, 2:1]", filtered.formatString())
    }
}
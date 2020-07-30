package coverosR3z.persistence


const val MAX_NUMBER_OF_FILES_PER_DIRECTORY = 1000
const val MAX_NUMBER_OF_DIRECTORIES = 1000
const val MAX_NUMBER_OF_TIMEENTRIES_PER_FILE = 100

/**
 * Writes the database to disk.
 *
 * It doesn't simply write the whole thing in one big file.
 * Rather, it splits things out a bit.
 *
 * Employees goes into its own file
 *
 * Projects does too
 *
 * TimeEntries does as well, except that the number of
 * those could be huge, so to make things performant and
 * manageable, we do this:
 *
 * Firstly, each file has up to [MAX_NUMBER_OF_TIMEENTRIES_PER_FILE] entries
 *  they are named like this:
 *       <FIRST_ID>.json
 *
 * We have two layers of directories:
 *
 * The directory that holds files can have up to [MAX_NUMBER_OF_FILES_PER_DIRECTORY] files
 * the layer above holds up to [MAX_NUMBER_OF_DIRECTORIES] directories
 *
 * Each directory is just labeled with an incrementing integer
 *
 * Therefore, it would look like this, as a tree:
 *    <DATABASE_NAME>_
 *                    |_1_
 *                    |   |_1.json
 *                    |   |_101.json
 *                    |   |_...
 *                    |
 *                    |_2_
 *                    |   |_100001.json
 *                    |   |_100101.json
 *                    |
 *                    |_...
 *
 *  This means we can hold up to this many time entries:
 *  MAX_NUMBER_OF_FILES_PER_DIRECTORY * MAX_NUMBER_OF_DIRECTORIES * MAX_NUMBER_OF_TIMEENTRIES_PER_FILE
 *
 *  The time entries are ordered in the files by their id.
 */
fun writeDbToDisk(pmd: PureMemoryDatabase) {
    TODO("Not yet implemented")
}

fun readDbFromDisk() : PureMemoryDatabase {
    TODO("Not yet implemented")
}
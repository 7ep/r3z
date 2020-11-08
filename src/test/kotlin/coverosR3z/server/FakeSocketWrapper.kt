package coverosR3z.server

class FakeSocketWrapper (
     var writeBehavior : () -> Unit = {},
     var readLineBehavior : () -> String = {""},
     var readBehavior : () -> String = {""}
    ) : ISocketWrapper {

    override fun write(input: String) {
        writeBehavior()
    }

    override fun readLine(): String {
        return readLineBehavior()
    }

    override fun read(len: Int): String {
        return readBehavior()
    }
}
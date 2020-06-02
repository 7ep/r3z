package r3z

class App {
    val greeting: String
        get() {
            return "Hello world."
        }

    fun thing() : String {
        fun inner1() : String {
            fun inner2() : String {
                fun inner3() : String {
                    fun inner4() : String {
                        return "FOO!!!"
                    }
                    return inner4()
                }
                return inner3()
            }
            return inner2()
        }
        return inner1()
    }
}

fun main(args: Array<String>) {
    println(App().greeting)
    println(App().thing())
}

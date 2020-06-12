package r3z.domainobjects

class User(val id: Int) {
    init {
        assert(id < 100_000_000) { "There is no way on earth this company has ever or will " +
                "ever have more than even a million employees" }
    }
}



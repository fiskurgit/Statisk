package fisk.staticsite

data class Link(val date: String, val title: String, val link: String, val year: Int, val month: Int, val day: Int): Comparable<Link>{

    companion object {
        private val COMPARATOR =
            Comparator.comparingInt<Link> { it.year }
                .thenComparingInt { it.month }
                .thenComparingInt { it.day }
    }

    override fun compareTo(other: Link): Int {
        return COMPARATOR.compare(this, other)
    }
}
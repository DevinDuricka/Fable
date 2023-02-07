package one.fable.fable.utils.extensions

fun MutableList<Long>.appendDurationSummation(nextItemDuration : Long){
    this.add(nextItemDuration + (this.lastOrNull() ?: 0L))
}
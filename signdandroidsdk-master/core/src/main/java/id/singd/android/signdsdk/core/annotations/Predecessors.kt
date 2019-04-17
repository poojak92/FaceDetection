package id.singd.android.signdsdk.core.annotations

import id.singd.android.signdsdk.core.Property

@Target(AnnotationTarget.CLASS)
annotation class Predecessors(val predecessors: Array<Property>) {
}
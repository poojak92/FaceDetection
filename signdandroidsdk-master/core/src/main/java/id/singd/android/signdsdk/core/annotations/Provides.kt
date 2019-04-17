package id.singd.android.signdsdk.core.annotations


import id.singd.android.signdsdk.core.Property

@Target(AnnotationTarget.CLASS)
annotation class Provides(val provides: Array<Property>) {
}
rootProject.name = "LogiCore"

val modules = listOf(
    "core-logistics",
    "routing-engine"
)

modules.forEach { module ->
    if (file(module).exists()) {
        include(":$module")
    }
}


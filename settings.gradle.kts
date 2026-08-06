rootProject.name = "SCL3"
include(
    "SCL",
    "SCLCore",
    "SCLBoot"
)

val minecraftLibraries = listOf("SCLTransformerDiscoveryService", "SCLMultiMCBootstrap")
include(minecraftLibraries)

for (library in minecraftLibraries) {
    project(":$library").projectDir = file("minecraft/libraries/$library")
}

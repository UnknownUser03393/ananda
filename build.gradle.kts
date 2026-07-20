plugins {
	kotlin("jvm") version "2.3.0"
	application
}

group = "dev.unknownuser"
version = "1.0-SNAPSHOT"

repositories {
	maven("https://maven.aliyun.com/repository/central")
	mavenCentral()
}

dependencies {
	val skikoVersion = "0.144.6"
	val skikoTarget = when {
		System.getProperty("os.name").startsWith("Mac", ignoreCase = true) && System.getProperty("os.arch") in setOf("aarch64", "arm64") -> "macos-arm64"
		System.getProperty("os.name").startsWith("Mac", ignoreCase = true) -> "macos-x64"
		System.getProperty("os.name").startsWith("Windows", ignoreCase = true) -> "windows-x64"
		else -> "linux-x64"
	}

	implementation("org.jetbrains.skiko:skiko-awt-runtime-$skikoTarget:$skikoVersion")
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("com.materialkolor:material-color-utilities:4.1.1")
	testImplementation(kotlin("test"))
}

kotlin {
	jvmToolchain(23)
}

application {
	mainClass.set("dev.unknownuser.ananda.remote.RemoteClickGuiKt")
}

tasks.test {
	useJUnitPlatform()
}

tasks.register<JavaExec>("renderSkiaTextComparison") {
	group = "verification"
	description = "Render the Skia half of the text comparison image."
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("dev.unknownuser.ananda.tools.TextComparisonKt")
}

tasks.register<JavaExec>("matcherDemo") {
	group = "application"
	description = "Run the typed event matcher demo."
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("dev.unknownuser.MatcherDemoKt")
}

tasks.register<JavaExec>("dynamicIslandDemo") {
	group = "application"
	description = "Run the Dynamic Island demo."
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("dev.unknownuser.DynamicIslandDemoKt")
}

tasks.register<JavaExec>("appControlsDemo") {
	group = "application"
	description = "Run the application controls and settings demo."
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("dev.unknownuser.AppControlsDemoKt")
}

tasks.register<JavaExec>("material3Demo") {
	group = "application"
	description = "Run the Material 3, responsive layout, and CJK text demo."
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("dev.unknownuser.Material3DemoKt")
}

tasks.register<JavaExec>("liquidBounceWidgetsDemo") {
	group = "application"
	description = "Run LiquidBounce src-theme widget recreations in Ananda."
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("dev.unknownuser.LiquidBounceWidgetsDemoKt")
}

tasks.register<JavaExec>("renderSkiaSubpixelComparison") {
	group = "verification"
	description = "Render Skia font edging and pixel geometry variants."
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("dev.unknownuser.ananda.tools.SkiaSubpixelComparisonKt")
}

val directWriteBuildDir = layout.buildDirectory.dir("native/directwrite")
val directWriteDll = directWriteBuildDir.map { it.file("Release/directwritejni.dll") }

tasks.register<Exec>("configureDirectWriteJni") {
	group = "build"
	description = "Configure the DirectWrite JNI native library with CMake."
	inputs.file("native/directwrite/CMakeLists.txt")
	inputs.file("native/directwrite/directwrite_jni.cpp")
	outputs.dir(directWriteBuildDir)
	commandLine(
		"cmake",
		"-S", "native/directwrite",
		"-B", directWriteBuildDir.get().asFile.absolutePath,
		"-G", "Visual Studio 17 2022",
		"-A", "x64"
	)
}

tasks.register<Exec>("buildDirectWriteJni") {
	group = "build"
	description = "Build the DirectWrite JNI native library."
	dependsOn("configureDirectWriteJni")
	inputs.file("native/directwrite/directwrite_jni.cpp")
	outputs.file(directWriteDll)
	commandLine(
		"cmake",
		"--build", directWriteBuildDir.get().asFile.absolutePath,
		"--config", "Release"
	)
}

tasks.register<JavaExec>("renderDirectWriteTextComparison") {
	group = "verification"
	description = "Render a Skia vs DirectWrite comparison image."
	dependsOn("buildDirectWriteJni")
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("dev.unknownuser.ananda.tools.DirectWriteComparisonKt")
}

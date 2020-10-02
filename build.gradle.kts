
allprojects {
    group = "xyz.cssxsh.dlsite"
    version = "1.0-SNAPSHOT"
    repositories {
        maven(url = "https://maven.aliyun.com/repository/releases")
        maven(url = "https://mirrors.huaweicloud.com/repository/maven")
        // bintray dl.bintray.com -> bintray.proxy.ustclug.org
        maven(url = "https://bintray.proxy.ustclug.org/kotlin/kotlinx/")
        // central
        maven(url = "https://maven.aliyun.com/repository/central")
        mavenCentral()
        // jcenter
        maven(url = "https://maven.aliyun.com/repository/jcenter")
        jcenter()
        mavenCentral()
    }

}





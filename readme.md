# DLsite DownLoader
一个基于kotlin协程的DLsite 下载器

## 使用说明

本程序基于JVM，需要安装JDK1.8及其以上环境。<br/>

首次运行后，会在工作目录生成配置文件`config.json` <br/>
请修改文件中login_id 和 password的值

命令行运行指令为`java -jar dlsite-downloader.jar VJ013799 RJ218195 ...` <br/>
YYXXXXXX等为dlsite作品ID <br/>

当命令行运行没有参数或者直接双击JAR时，将会根据配置文件下载指定列表的作品

## 配置文件 config.json

| 名称               | 默认值                                 | 作用                 |
| ------------------ | -------------------------------------- | -------------------- |
| style              | `LIGHT`                                | 用于图形界面设置     |
| download_list_name | `Download`                             | 要自动下载的列表名称 |
| login_id           |                                        | 登录名，一般为邮箱   |
| cname              |                                        | 本地cname域名        |
| china_dns          | `https://223.5.5.5/dns-query`          | 中国DNS解析更快的IP  |
| foreign_dns        | `https://cloudflare-dns.com/dns-query` | 国外DNS避免污染      |
| max_async_num      | `16`                                   | 最大协程数量         |
| block_size_MB      | `8`                                    | 缓存块大小，单位MB   |

## 其他说明

目前程序只有控制台版本
基于JavaFX的图形化版本正在开发中
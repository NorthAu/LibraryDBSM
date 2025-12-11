# 学校图书借阅管理系统（Java 21 + MySQL）

本项目给出了使用 **Java 21** 与 **MySQL** 构建图书借阅管理系统的最小示例，包括数据库建模、触发器、视图、存储过程以及基于 JDBC 的服务层代码。

## 环境准备
- JDK 21（例如 Temurin 21）
- Maven 3.9+
- MySQL 8.0+
- 可选：Docker（快速启动 MySQL）

### 本地 MySQL 快速启动（可选）
```bash
docker run -d --name library-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=secret mysql:8.0
```

## 初始化数据库
执行 `db/schema.sql` 创建数据库、表、触发器、视图与存储过程，并写入示例数据：
```bash
mysql -h localhost -u root -p < db/schema.sql
```
默认创建的业务账户：`library_admin / library_admin`。

## 运行示例代码
1. 设定数据库连接（可通过环境变量覆盖）：
   - `DB_URL`：默认 `jdbc:mysql://localhost:3306/library_db?useSSL=false&serverTimezone=UTC`
   - `DB_USER`：默认 `library_admin`
   - `DB_PASSWORD`：默认 `library_admin`
2. 编译并运行：
```bash
mvn -q package
java -cp target/library-dbsm-1.0.0.jar:$(dependency:list -DincludeTypes=jar -DoutputAbsoluteArtifactFilename -DincludeScope=runtime -DexcludeTransitive -DappendOutput=true 2>/dev/null | awk '{print $NF}' | paste -sd: -) com.library.App
```
运行时会示例化读者、图书并演示借阅、续借、归还与罚金计算。

## 项目结构
```
pom.xml                      # Maven 项目配置，依赖 MySQL 驱动、HikariCP、SLF4J
src/main/java/com/library/   # Java 代码
  ├─ App.java                # 演示入口
  ├─ config/DatabaseManager  # HikariCP 数据源配置
  ├─ model/                  # 记录类型定义（Book/Reader/Loan/...）
  ├─ repository/             # JDBC 持久层，封装借阅、续借、归还查询
  └─ service/                # 业务层，处理罚款等业务规则
src/main/resources/          # 预留资源目录
/db/schema.sql               # 数据库初始化脚本（表、触发器、视图、存储过程、示例数据）
```

## 关键功能说明
- **信息管理**：`books`、`categories`、`publishers`、`readers` 表以及 `LibraryRepository#insertBook/insertReader` 完成图书、类别、出版社、读者、借阅证信息维护。
- **借阅/续借/归还**：`LibraryRepository#borrowBook`、`renewLoan`、`returnBook` 以及 `LibraryService` 中的罚金计算。
- **罚款与收款**：`LibraryService#returnBook` 计算超期罚款；`payments` 表可记录收款流水。
- **触发器**：`trg_loans_insert`、`trg_loans_update` 在借书与还书时自动更新 `books.available_copies`。
- **视图**：`view_book_stock` 展示书号、书名、总数及在册数。
- **存储过程**：`get_reader_loans` 查询指定读者的借阅情况。
- **参照完整性**：所有外键约束保证数据一致性。

## 提示
- 如需与现有系统集成，可在 `LibraryService` 中扩展更多校验（如读者证有效期、借阅上限等）。
- 业务规则均以简化示例呈现，可按需要替换为框架（如 Spring/JPA）。

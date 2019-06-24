B+树索引

#### 起因

通过InnoDB数据页结构可知所有数据页组成双链表结构，每个数据页中的记录按照主键大小组成单向链表，根据主键查询某条记录是从页目录通过二分法比较主键大小找到对应的槽，再遍历槽中的记录找到目标记录。但是主键查找有方法，那其他的列查找又怎么实现。如果通过从最小记录开始依次遍历，很明显很傻，所以引出了索引的概念。

#### 过程

以InnoDB为例，假设某表采用compact为行格式存储记录，那么可知其中一条记录包含几个部分：record type，next record，各列的值，其他信息；主要看record type，0表示普通记录/1表示目录项记录/2表示最小记录/3表示最大记录，那么在数据页中一条记录的存在如下图：

![image_1caadhc4g1pb7hk81fcd4vt1u6r3t.png-79.8kB](https://user-gold-cdn.xitu.io/2019/4/9/16a01bd1be0d43ce?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)



##### 而添加数据记录涉及到页的过程如下：

1. 插入一条记录到表中，而页10已经满了（页内容量很大，可以存好多记录）
2. 那么分配一个新页28，（双向链表结构下，页号是不连续的，存储空间也不是挨着的）
3. 这时发现页10最大主键为10，插入记录主键为8，但是为方便查找，下一页的最小主键必须大于上页的最大记录，所以需要移动主键为10的记录到页28，主键8的记录到页10.这个过程即页分裂，在新增页的时候发生。
4. 因为页数很多，所以为更快定位记录所在页，将设一个目录（也称索引），数据页为目录项，目录项存储是一个连续存储空间，目录项包括数据页最小的主键值key和数据页页号，那么插叙某条记录时就可以用二分法从目录项中确定目标记录所在目录项，后遍历目录项即数据页中的记录
5. 有两个问题：1.随着页数的增长目录项也随之增长，存储空间没有连续这么大的；2.如果删除某页中的全部记录，那么这个目录项随之删除。这就需要把该目录项后面的所有目录项都前移，比较麻烦
6. 为解决上述问题，将新分配一个页专门存储目录项记录，这个页与普通记录存储页相似；查找目标记录的方式与之前的相同，依旧以二分法为主。
7. 因存储目录项记录的页大小有限制，所以会有多个页来存储目录项；但是这些页在存储空间不是挨着的，那么怎么能快速定位目标记录存储在某个目录项记录的页，为了加速查找，可以将存储目录项的页再生成一个更高级的目录即大目录。

![image_1cacafpso19vpkik1j5rtrd17cm3a.png-158.1kB](https://user-gold-cdn.xitu.io/2019/4/9/16a01bd2a6c7a65f?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

一层一层下来就是一个树状即B+树，树由叶节点和根节点组成，最底下一层存储完整且实际的用户记录，称为叶节点；往上存放目录项的节点称为非叶节点或内节点，最顶上的称为根节点。那么查找目标记录同样也可采用二分查找法。

#### 定义

##### 聚簇索引

InnoDB会自动为我们创建聚簇索引，且聚簇索引就是数据的存储方式，核心在主键

1. B+树的叶子节点存储的是完整的用户记录
2. 页内记录/存放用户记录的页/存放目录项的页都是使用记录主键值的大小进行记录和排序的

##### 二级索引

因聚簇索引的存储方式以主键大小排序，如果对其他列有查询要求，那么可以为其他列建索引B+树；但是和聚簇索引不同，这个B+树的叶子节点只存放查询列+主键的值，目录项存储列+页号+主键值；其他均以该列的大小顺序为主，排序方式和表用到的字符集有关，如果找到目标记录所在节点就会根据主键值去聚簇索引中查找完整的用户记录，即回表。

##### 联合索引

以多个列的大小为排序顺序，即同时为多个列建索引，但只有一个B+树。记录项由列1，列2...，页号组成，叶子节点由列1，列2....和主键组成

#### 代价

- 空间：索引数中一个页的大小为16kb，而索引树由很多个目录页组成
- 时间
  - 增删改产生页分裂
  - 回表：在从二级索引树中找到结果时需要根据结果给出的主键从聚簇索引树中找到完整的用户记录
    - 问题：用到两个索引：二级索引，聚簇索引；访问二级索引使用顺序I/O，访问聚簇索（全表扫描）引使用随机I/O
    - 解决：覆盖索引：在查询列中仅包含索引列，拒绝回表

#### 使用

假设一个表user有四个字段id，name，phone，birthday，address；现有id作为主键则有对应的聚簇索引，并为name，phone，birthday作联合索引idx_name_phone_birthday；

- 全值匹配：

  - select * from user where name ='aa' and birthday ='1990-10-19' and phone ='123'

  查找过程为：先定位B+中name所在位置，name相同的列中按照phone的大小进行排序，如果查到name和phone的大小相同，则在该列下按birthday大小排序；where后的条件可不按顺序排列，因为mysql在查询前会对SQL进行优化

- 匹配左边的列：

  - select * from user where name ='aa' and birthday ='1990-10-19'（用到索引）
  - select * from user where  birthday ='1990-10-19'（用不到索引）；因为索引树查找是先从name开始，是不能跳过name的

- 匹配列前缀：

  - select * from user where name  like 'aa%' （用到索引）
  - select * from user where name  like '%aa' （用不到索引）因为索引大小的排序是按照字符集对应的排序规则，这样无法按照前缀大小排序只能全表扫描

- 匹配范围值：

  - SELECT * FROM user WHERE name > 'Asa' AND name < 'Barlow' AND birthday > '1980-01-01';只有在对索引的最左边的那个列进行范围查找的时候才能用到B+树，即birthday的查找是在name使用索引查找的结果记录中作为过滤条件而不是在结果中按照birthday列进行排序

- 精确匹配某一列并范围匹配另外一列：

  - SELECT * FROM user WHERE name = 'Ashburn' AND phone > '15100000000' AND birthday > '1980-01-01' AND birthday < '2000-12-31' ;（B+树中name值相同的列下，继续按照phone的大小排序，但是因为phone范围查找下的值可能phone不相同（不好排序），所以不能用B+树索引了，只能遍历phone结果查询）；
  - SELECT * FROM user WHERE name = 'Ashburn' AND phone = '123' AND birthday > '1980-01-01';（用到索引）

- 用于排序（没有索引排序默认使用文件排序filesort；order by不指定默认使用升序）

  - 顺序匹配：SELECT * FROM user  ORDER BY name,  phone,birthday LIMIT 10;（用到索引）
  - 顺序匹配：SELECT * FROM user  WHERE name = 'A' ORDER BY phone,birthday LIMIT 10;（用到索引）
  - 匹配左边：SELECT * FROM user  ORDER BY name,  phone（用到索引）
  - 升降序混用：SELECT * FROM user  ORDER BY name,  phone desc（用到文件排序）
  - where子句中出现非索引字段：SELECT * FROM user where adrress ='hdeuh' ORDER BY name,  phone（用不到索引）一般先提取address字段再排序，但address没有索引
  - 排序出现非索引字段：SELECT * FROM user  ORDER BY name,  address（用不到索引）
  - 排序列使用复杂表达式：SELECT * FROM user  ORDER BY name,  count(phone)（用不到索引列）保证索引列出现是单独出现而不是修饰过

- 用于分组（没有分组默认内存内实现）

  - 顺序匹配/左边匹配：SELECT name, birthday, phone, COUNT(*) FROM user GROUP BY name, phone, birthday（用到索引）

#### 索引选择

- 搜索/排序/分组列
- 基数大的列：基数（一个列中不重复的数据）大，说明重复数据少范精准度高
- 列类型小：比如能使用int就不用bigint，因为数据类型越小，查询时比较速度越快，索引占的空间越小，一个数据页可存放的内容越多，能够放在缓存的数据页越多，减少i/o带来的性能损耗，加快读写效率
- 索引字符串值的前缀：如果索引列很长，可以在建立索引时只保留记录的的前10个字符的编码KEY idx_name_birthday_phone (name(10), birthday, phone)；但是这样不支持在索引中排序
- 大小依次增长的列：比如主键
- 冗余索引：不需要重复为一个列建立索引，这样只会增加维护成本
- 索引列在表达式中单独存在，而不是以某个表达式

######  todo 

innoDB根页面不动，索引注意事项

myisam中索引方案
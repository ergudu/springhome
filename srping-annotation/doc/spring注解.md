# spring注解详解

## @Conditional

按照一定条件进行判断，满足条件的给容器注册Bean

```java
public class WindowCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        String property = environment.getProperty("os.name");
        return property.contains("Windows");
    }
}

public class LinuxCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        String property = environment.getProperty("os.name");
        return property.contains("linux");
    }
}


//@Conditional({WindowCondition.class})
@Configuration
public class MainConfig {

    @Bean
    public Person person(){
        return new Person(10,"小那个好");
    }

    @Conditional({WindowCondition.class})
    @Bean("bill")
    public Person person2(){
        return new Person(65,"Bills");
    }

    @Conditional({LinuxCondition.class})
    @Bean("linus")
    public Person person3(){
        return new Person(48,"Linus");
    }
}
```

标注在类上时表示：当条件满足时里面的bean才能注册到容器中



给容器中注册组件的方式：

1）包扫描+组件注解（@Controller/@Service/@Repository/@Component）

2）@Bean [导入第三方里面的组件]

3）@Import [快速给容器中添加一个组件]

4）使用spring提供的FactoryBean（工厂Bean）

​	! 默认获取到的是工厂bean调用getObeject创建的对象，想要获取工厂bean本身需要在id前加个&标识

## @Import

```java
//Color组件
public class Color {
}

//第一种导入组件的方式
@Configuration
public class MainConfig {

    @Bean
    public Color color(){
        return new Color();
    }
}

//第二种导入组件的方式
@Configuration
@Import({Color.class}) // id是组件的全类名
public class MainConfig {
    
}
```



@Import使用ImportSelector

```java
@Configuration
@Import({Color.class, Red.class,MyImportSelector.class})
public class MainConfig {

}

//自定义逻辑返回需要的组件
public class MyImportSelector implements ImportSelector {
    //返回值，就是要导入容器中的组件的全类名
    //AnnotationMetadata:当前标注@Import注解的类的所有注解信息
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {

        //不要返回null，因为会报错
        return new String[]{"com.three.beans.Yellow","com.three.beans.Blue"};
    }
}

package com.three.beans;
public class Red {
}
public class Blue {
}
public class Yellow {
}

```

@Import使用ImportBeanDefinitionRegistrar

```java
@Configuration
@Import({Color.class, Red.class,MyImportSelector.class, MyImportBeanDefinitionRegistrar.class})
public class MainConfig {

}


public class MyImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        boolean red = registry.containsBeanDefinition("com.three.beans.Blue");
        boolean blue = registry.containsBeanDefinition("com.three.beans.Red");
        if(red && blue){
            //指定Bean的定义信息
            RootBeanDefinition beanDefinition = new RootBeanDefinition(RainBow.class);
            //容器中注册一个Bean，并指定Bean名字为rainBow
            registry.registerBeanDefinition("rainBow",beanDefinition);
        }
    }
}

package com.three.beans;
public class RainBow {
}

```

## FactoryBean

```java
//工厂Bean
public class ColorFactoryBean implements FactoryBean<Color> {
   //返回一个Color对象，这个对象会添加到容器中
    @Override
    public Color getObject() throws Exception {
        return new Color();
    }

    @Override
    public Class<?> getObjectType() {
        return Color.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}


@Configuration
@Import({Color.class, Red.class,MyImportSelector.class, MyImportBeanDefinitionRegistrar.class})
public class MainConfig {

    @Bean
    public ColorFactoryBean colorFactoryBean(){
        return new ColorFactoryBean();
    }
}


public class HellTest {
    @Test
    public void hello(){
        AnnotationConfigApplicationContext ctx =new AnnotationConfigApplicationContext(MainConfig.class);
        Object bean = ctx.getBean("colorFactoryBean");
        System.out.println(bean.getClass().getName());
        //输出:com.three.beans.Color

        bean = ctx.getBean("&colorFactoryBean");
        System.out.println(bean.getClass().getName());
        //输出:com.three.beans.ColorFactoryBean
    }
}

//加&前缀的原因请看BeanFactory源码
public interface BeanFactory {
    String FACTORY_BEAN_PREFIX = "&";
 	.....   
}
```

## Bean的生命周期

bean生命周期：

​		bean创建-----初始化------销毁

容器管理bean的生命周期；

BeanPostProcessor.postProcessBeforeInitialization()

初始化：

​	在对象创建完成，并初始化完属性后，调用初始化方法

BeanPostProcessor.postProcessAfterInitialization()

销毁：

​	单实例：容器关闭的时候

​	多实例：容器不会管理这个bean，容器不会调用销毁方法

我们可以自定义bean的初始化和销毁方法，容器执行到指定生命周期时就会调用我们定义的方法。

1）指定初始化和销毁方法：通过@Bean指定initMethod和destroyMethod

```JAVA
public class Car {
    public Car(){
        System.out.println("Car constructor............");
    }

    public void init(){
        System.out.println("Car..........init..........");
    }

    public void destory(){
        System.out.println("Car..........destory..........");
    }
}

@Configuration
public class MainConfigOfLifeCycle {

    @Bean(initMethod = "init", destroyMethod = "destory")
    public Car car() {
        return new Car();
    }
}

public class HellTest {
    @Test
    public void hello() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfigOfLifeCycle.class);
        System.out.println("容器创建完成.....................");
        ctx.close();
        System.out.println("容器关闭.....................");
    }
}
//打印如下
Car constructor............
Car..........init..........
容器创建完成.....................

Car..........destory..........
容器关闭.....................
```

2）通过让Bean实现InitializingBean（定义初始化逻辑）

​						 实现DisposableBean（定义销毁逻辑）

```java
public class Car implements InitializingBean, DisposableBean {
    public Car() {
        System.out.println("Car constructor............");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("Car................afterPropertiesSet...............");
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("Car................destroy...............");
    }

}

@Configuration
public class MainConfigOfLifeCycle {

    @Bean
    public Car car() {
        return new Car();
    }
}


public class HellTest {
    @Test
    public void hello() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfigOfLifeCycle.class);
        System.out.println("容器创建完成.....................");
        ctx.close();
        System.out.println("容器关闭.....................");
    }
}
//打印如下
Car constructor............
Car................afterPropertiesSet...............
容器创建完成.....................

Car................destroy...............
容器关闭.....................
```

3）可以使用JSR250;

​		@PostConstruct

​		@PreDestroy

```java
public class Car {
    public Car() {
        System.out.println("Car constructor............");
    }

    //对象创建完成并且属性初始化后执行这个方法
    @PostConstruct
    public void init() {
        System.out.println("Car................init...............");
    }

    //容器移除对象之前调用这个方法
    @PreDestroy
    public void destroy() {
        System.out.println("Car................destroy...............");
    }

}

@Configuration
public class MainConfigOfLifeCycle {

    @Bean
    public Car car() {
        return new Car();
    }
}

public class HellTest {

    @Test
    public void hello() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfigOfLifeCycle.class);
        System.out.println("容器创建完成.....................");
        ctx.close();
        System.out.println("容器关闭.....................");
    }
}

//打印如下
Car constructor............
Car................init...............
容器创建完成.....................
    
Car................destroy...............
容器关闭.....................
```

4）bean后置处理器BeanPostProcessor,在bean初始化方法前后做一些处理工作

​		postProcessBeforeInitialization：初始化方法之前执行

​		postProcessAfterInitialization：初始化方法之后执行

```java
public class Car {
    public Car() {
        System.out.println("Car constructor............");
    }

    //对象创建完成并且属性初始化后执行这个方法
    @PostConstruct
    public void init() {
        System.out.println("Car................init...............");
    }

    //容器移除对象之前调用这个方法
    @PreDestroy
    public void destroy() {
        System.out.println("Car................destroy...............");
    }

}

public class MyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
        System.out.println("BeanPostProcessor..................postProcessBeforeInitialization");
        return o;
    }

    @Override
    public Object postProcessAfterInitialization(Object o, String s) throws BeansException {
        System.out.println("BeanPostProcessor..................postProcessAfterInitialization");
        return o;
    }
}


@Configuration
public class MainConfigOfLifeCycle {

    @Bean
    public Car car() {
        return new Car();
    }

    @Bean
    public MyBeanPostProcessor myBeanPostProcessor() {
        return new MyBeanPostProcessor();
    }
}

public class HellTest {

    @Test
    public void hello() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfigOfLifeCycle.class);
        System.out.println("容器创建完成.....................");
        ctx.close();
        System.out.println("容器关闭.....................");
    }
}
//执行如下
Car constructor............
BeanPostProcessor..................postProcessBeforeInitialization
Car................init...............
BeanPostProcessor..................postProcessAfterInitialization
容器创建完成.....................
    
Car................destroy...............
容器关闭.....................    
```

## @PropertySource 加载外部配置文件



## @Value 属性赋值

这个是没有使用@Value

```java
package com.three.beans;

public class Person {
    private Integer id;
    private String name;

    public Person() {
    }

    public Person(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}

@Configuration
public class MainConfigPropertyValue {

    @Bean
    public Person person() {
        return new Person();
    }
}


public class HellTest {

    @Test
    public void hello() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfigPropertyValue.class);
        Person person = ctx.getBean(Person.class);
        System.out.println(person);
    }
}
//属性没有赋值前输出：Person{id=null, name='null'}
```

这个是使用@Value

person.properties

```properties
person.nickName=小张三
```

```java
public class Person {
    //使用@Value属性赋值
    //1、基本数值
    //2、可以写SpEL: #{}
    //3、可以写${}：获取配置文件中的值(在运行环境变量里面的值)
    @Value("#{10-1}")
    private Integer id;
    @Value("张三")
    private String name;
    @Value("${person.nickName}")
    private String nickName;
 
    ....
}


//使用@PropertySource读取外部配置文件中k/v保存到运行的环境变量中
@PropertySource(value = {"classpath:/person.properties"})
@Configuration
public class MainConfigPropertyValue {

    @Bean
    public Person person() {
        return new Person();
    }
}

 @Test
    public void hello() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfigPropertyValue.class);
        Person person = ctx.getBean(Person.class);
        System.out.println(person);

        ConfigurableEnvironment env = ctx.getEnvironment();
        String property = env.getProperty("person.nickName");
        System.out.println(property);
    }
//输出如下：
//Person{id=9, name='张三', nickName='小张三'}
//小张三
```

## @Primary

当有多个对应的属性是可以通过@Primary来指定优先加载

```java

@Repository
public class PersonDao {
    private String lable;

    public PersonDao() {
        lable = "d1";
    }

    public String getLable() {
        return lable;
    }

    public void setLable(String lable) {
        this.lable = lable;
    }

    @Override
    public String toString() {
        return "PersonDao{" +
                "lable='" + lable + '\'' +
                '}';
    }
}


@Configuration
@ComponentScan({"com.three.dao","com.three.service"})
public class MainConfigPropertyValue {

    @Primary
    @Bean("personDao2")
    public PersonDao personDao(){
        PersonDao dao = new PersonDao();
        dao.setLable("d2");
        return new PersonDao();
    }
}


@Service
public class PersonService {
    @Autowired
    private PersonDao personDao;

    public void print() {
        System.out.println("person lable=" + personDao.getLable());
    }
}
```

上面容器中会有两个PersonDao类型，但有个上面指定@Primary，则在@Autowired自动装配是会优先装配标注@Primary的。如果想装配其他的可以@Autowired+@Qualifier("id名字")来指定装配的对象



@Autowired可以标注在方法、构造器、参数、属性上，当标注在构造器（有参构造器）上时，如果只有这一个构造器可以省略@Autowired注解，参数位置的组件还是会从容器中加载



@Bean+方法参数：参数从容器中获取 默认不写@Autowired

## 自动装配

1)、@Autowired + @Qualifier + @Primary

2)、@Resource (JSR255规范)

3)、@Inject (JSR330规范)

4)、实现Aware接口注入spring底层组件

5)、@Profile  根据环境注册Bean

## profile

spring为我们提供的可以根据当前环境，动态的激活和切换一系列组件的功能。

@Profile:指定组件在那个环境的情况下才能被注册到容器中，不指定，在任何环境下都能注册这个组件

加了环境标识的Bean，只有这个环境被激活的时候才能注册到容器中。默认是default环境

当@Profile标注在配置类上时，只有是指定环境的时候，整个配置类里面的所有配置才能开始生效

怎样切换环境呢？

1、使用命令行动态参数  -Dspring.profiles.active=test/dev/prod

2、使用代码的方式

dbcofig.properties

```properties
db.user=root
db.password=root
db.driverClass=com.jdbc.mysql.Driver
```

```java

@PropertySource({"classpath:/dbconf.properties"})
@Configuration
public class MainConfigOfProfile implements EmbeddedValueResolverAware {

    @Value("${db.user}")
    private String user;

    private StringValueResolver resolver;
    private String driverClass;

    @Bean
    public Person person(){
        return new Person();
    }

    @Profile("test")
    @Bean("testDataSource")
    public DataSource dataSourceTest(@Value("${db.password}") String pwd) throws PropertyVetoException {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setUser(user);
        dataSource.setPassword(pwd);
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/test");
        dataSource.setDriverClass(driverClass);
        return dataSource;
    }

    @Profile("dev")
    @Bean("devDataSource")
    public DataSource dataSourceDev(@Value("${db.password}") String pwd) throws PropertyVetoException {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setUser(user);
        dataSource.setPassword(pwd);
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/dev");
        dataSource.setDriverClass(driverClass);
        return dataSource;
    }

    @Profile("prod")
    @Bean("prodDataSource")
    public DataSource dataSourceProd(@Value("${db.password}") String pwd) throws PropertyVetoException {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setUser(user);
        dataSource.setPassword(pwd);
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/prod");
        dataSource.setDriverClass(driverClass);
        return dataSource;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
        this.driverClass = this.resolver.resolveStringValue("${db.driverClass}");
    }
}
```

```java

public class HellTest {

    @Test
    public void hello() {
        //1、创建一个ApplicationContext,思考：为什么要无参呢？
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        //2、设置需要激活的环境,可以设置多个环境同时生效
        ctx.getEnvironment().setActiveProfiles("test", "dev");
        //3、注册配置类
        ctx.register(MainConfigOfProfile.class);
        //4、启动刷新容器
        ctx.refresh();

        String[] names = ctx.getBeanNamesForType(DataSource.class);
        Stream.of(names).forEach(System.out::println);

        Person person = ctx.getBean(Person.class);
        System.out.println(person);
    }
}
//输出如下：
//testDataSource
//devDataSource
//Person{id=9, name='张三'}
```


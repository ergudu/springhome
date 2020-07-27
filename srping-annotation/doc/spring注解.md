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


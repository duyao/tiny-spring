package us.codecraft.tinyioc.aop;

import org.aopalliance.intercept.MethodInterceptor;
import us.codecraft.tinyioc.beans.BeanPostProcessor;
import us.codecraft.tinyioc.beans.factory.AbstractBeanFactory;
import us.codecraft.tinyioc.beans.factory.BeanFactory;

import java.util.List;

/**
 * @author yihua.huang@dianping.com
 */

//BeanPostProcessor ：在 postProcessorAfterInitialization 方法中，使用动态代理的方式，
// 返回一个对象的代理对象。解决了 在 IoC 容器的何处植入 AOP 的问题。
//BeanFactoryAware ：这个接口提供了对 BeanFactory 的感知，
// 这样，尽管它是容器中的一个 Bean，却可以获取容器的引用，进而获取容器中所有的切点对象，
// 决定对哪些对象的哪些方法进行代理。解决了 为哪些对象提供 AOP 的植入 的问题。
public class AspectJAwareAdvisorAutoProxyCreator implements BeanPostProcessor, BeanFactoryAware {

	private AbstractBeanFactory beanFactory;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws Exception {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
		if (bean instanceof AspectJExpressionPointcutAdvisor) {
			return bean;
		}
		if (bean instanceof MethodInterceptor) {
			return bean;
		}
		//拿到所有的PointcutAdvisor，切点通知器，记录了对哪个对象的哪个方法执行什么拦截
		//pointcut切点对象，可以取出ClassFilter 对象和一个 MethodMatcher 对象。
		// a.ClassFilter前者用于判断是否对某个对象进行拦截（用于 筛选要代理的目标对象）
		// b.MethodMatcher用于判断是否对某个方法进行拦截（用于 在代理对象中对不同的方法进行不同的操作）
		//advisor通知对象，实现具体的拦截方法
		List<AspectJExpressionPointcutAdvisor> advisors = beanFactory
				.getBeansForType(AspectJExpressionPointcutAdvisor.class);

		for (AspectJExpressionPointcutAdvisor advisor : advisors) {
			if (advisor.getPointcut().getClassFilter().matches(bean.getClass())) {
                ProxyFactory advisedSupport = new ProxyFactory();
				//这就是advice，执行什么操作
				advisedSupport.setMethodInterceptor((MethodInterceptor) advisor.getAdvice());
				//这就是pointcut对什么方法进行拦截
				advisedSupport.setMethodMatcher(advisor.getPointcut().getMethodMatcher());

				//生成目标
				TargetSource targetSource = new TargetSource(bean, bean.getClass(), bean.getClass().getInterfaces());
				advisedSupport.setTargetSource(targetSource);

				//生成代理对象，这里生成了cglib代理
				return advisedSupport.getProxy();
			}
		}
		return bean;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws Exception {
		this.beanFactory = (AbstractBeanFactory) beanFactory;
	}
}

package ubc.pavlab.rdp.server.util;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * General-purpose pointcuts to recognize CRUD operations etc.
 * 
 * @author paul
 * @version $Id: SystemArchitectureAspect.java,v 1.1 2012/09/01 01:42:29 cmcdonald Exp $
 */
@Aspect
public class SystemArchitectureAspect {

    /*
     * 
     * For help with expressions see http://static.springsource.org/spring/docs/2.5.x/reference/aop.html#6.2.3.4
     */

    /**
     * Encompasses the 'web' packages
     */
    /*
     * @Pointcut("within(ubc.pavlab.rdp.server..*)") public void inWebLayer() { }
     */

    /**
     * Methods that create new objects in the persistent store
     */
    @Pointcut("ubc.pavlab.rdp.server.util.SystemArchitectureAspect.daoMethod() && ( execution(* save(..)) || execution(* create*(..)) || execution(* findOrCreate(..)) || execution(* persist*(..))   )")
    public void creator() {//
    }

    @Pointcut("ubc.pavlab.rdp.server.util.SystemArchitectureAspect.deleter() ||ubc.pavlab.rdp.server.util.SystemArchitectureAspect.loader() || ubc.pavlab.rdp.server.util.SystemArchitectureAspect.creator() || ubc.pavlab.rdp.server.util.SystemArchitectureAspect.updater() || ubc.pavlab.rdp.server.util.SystemArchitectureAspect.deleter()")
    public void crud() {//
    }

    /**
     * This pointcut is used to apply audit and acl advice at DAO boundary.
     */
    @Pointcut("@target(org.springframework.stereotype.Repository) && execution(public * ubc.pavlab.rdp.server..*.*(..))")
    public void daoMethod() {//
    }

    /**
     * Methods that delete items in the persistent store
     */
    @Pointcut("ubc.pavlab.rdp.server.util.SystemArchitectureAspect.daoMethod() && (execution(* remove(..)) || execution(* delete*(..)))")
    public void deleter() {//
    }

    /**
     * Encompasses the 'model' packages
     */
    @Pointcut("within(ubc.pavlab.rdp.server..*)")
    public void inModelLayer() {
    }

    /**
     * Methods that load (read) from the persistent store
     */
    @Pointcut("ubc.pavlab.rdp.server.util.SystemArchitectureAspect.daoMethod() && (execution(* load(..)) || execution(* loadAll(..)) || execution(* loadSubset(..)) || execution(* read(..)))")
    public void loader() {//
    }

    /**
     * Create, delete or update methods
     */
    @Pointcut("ubc.pavlab.rdp.server.util.SystemArchitectureAspect.creator() || ubc.pavlab.rdp.server.util.SystemArchitectureAspect.updater() || ubc.pavlab.rdp.server.util.SystemArchitectureAspect.deleter()")
    public void modifier() {//
    }

    /**
     * A entity service method: a public method in a \@Service.
     */
    @Pointcut("@target(org.springframework.stereotype.Service) && execution(public * ubc.pavlab.rdp.server..*.*(..))")
    public void serviceMethod() {
        /*
         * Important document:
         * http://forum.springsource.org/showthread.php?28525-Difference-between-target-and-within-in-Spring-AOP
         * 
         * Using @target makes a proxy out of everything, which causes problems if services aren't implementing
         * interfaces -- seems for InitializingBeans in particular. @within doesn't work, at least for the ACLs.
         */
    }

    @Pointcut("@target(org.springframework.stereotype.Service) && (execution(public * ubc.pavlab.rdp.server..*.*(*)) || execution(public * ubc.pavlab.rdp.server..*.*(*,..)))")
    public void serviceMethodWithArg() {//
    }

    /**
     * Methods that update items in the persistent store
     */
    @Pointcut("ubc.pavlab.rdp.server.util.SystemArchitectureAspect.daoMethod() && execution(* update(..))")
    public void updater() {//
    }

}
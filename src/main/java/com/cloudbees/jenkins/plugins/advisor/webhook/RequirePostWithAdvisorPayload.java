package com.cloudbees.jenkins.plugins.advisor.webhook;


import com.google.common.base.Optional;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.Interceptor;
import org.kohsuke.stapler.interceptor.InterceptorAnnotation;
import org.slf4j.Logger;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import javax.servlet.ServletException;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.kohsuke.stapler.HttpResponses.error;
import static org.kohsuke.stapler.HttpResponses.errorWithoutStack;
import static org.slf4j.LoggerFactory.getLogger;

@Retention(RUNTIME)
@Target({METHOD, FIELD})
@InterceptorAnnotation(RequirePostWithAdvisorPayload.Processor.class)
public @interface RequirePostWithAdvisorPayload {
  class Processor extends Interceptor {
    private static final Logger LOGGER = getLogger(Processor.class);
    private static final String SIGNATURE_HEADER = "CloudBees-Advisor-Signature";

    @Override
    public Object invoke(StaplerRequest req, StaplerResponse rsp, Object instance, Object[] arguments)
            throws IllegalAccessException, InvocationTargetException, ServletException {
      shouldBePostMethod(req);
      shouldContainParseablePayload(arguments);
      shouldProvideValidSignature(req, arguments);

      return target.invoke(req, rsp, instance, arguments);
    }

    /**
     * Duplicates org.kohsuke.stapler.interceptor.RequirePOST precheck.
     * As of it can't guarantee order of multiply interceptor calls,
     * it should implement all features of required interceptors in one class
     *
     * @param request what should be a post
     * @throws InvocationTargetException if method is not POST
     */
    protected void shouldBePostMethod(StaplerRequest request) throws InvocationTargetException {
      if (!request.getMethod().equals("POST")) {
        throw new InvocationTargetException(error(SC_METHOD_NOT_ALLOWED, "Method POST required"));
      }
    }

    /**
     * Determine if this payload is valid.
     * 
     * @param arguments   arguments from payload
     */
    private void shouldContainParseablePayload(Object[] arguments) {
      for(int x=0; x< arguments.length; x++ ) {
        LOGGER.info("VALUE OF ARGS: " + arguments[x]);
        //isTrue should be called here
        //also check if the payload is null
      }
    }

    /**
     * Checks that an incoming request has a valid signature.
     *
     * @param req Incoming request.
     * @throws InvocationTargetException if any of preconditions is not satisfied
     */
    protected void shouldProvideValidSignature(StaplerRequest req, Object[] args) throws InvocationTargetException {
      Optional<String> signHeader = Optional.fromNullable(req.getHeader(SIGNATURE_HEADER));
      
      //Secret secret = GitHubPlugin.configuration().getHookSecretConfig().getHookSecret();

      if (signHeader.isPresent()){ //&& Optional.fromNullable(secret).isPresent()) {
        LOGGER.info("SIGNED HEADER: " + signHeader.get());
        /*String digest = substringAfter(signHeader.get(), SHA1_PREFIX);
        LOGGER.trace("Trying to verify sign from header {}", signHeader.get());
        isTrue(
                CalculateValidSignature.setUp(payloadFrom(req, args), secret).matches(digest),
                String.format("Provided signature [%s] did not match to calculated", digest)
        );*/
      } else {
          LOGGER.info("Apparently it was null");
      }
    }

            /**
         * Utility method to stop preprocessing if condition is false
         *
         * @param condition on false throws exception
         * @param msg       to add to exception
         *
         * @throws InvocationTargetException BAD REQUEST 400 status code with message
         */
        private void isTrue(boolean condition, String msg) throws InvocationTargetException {
          if (!condition) {
              throw new InvocationTargetException(errorWithoutStack(SC_BAD_REQUEST, msg));
          }
}
  }
}
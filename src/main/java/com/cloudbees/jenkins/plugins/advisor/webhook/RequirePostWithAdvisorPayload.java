package com.cloudbees.jenkins.plugins.advisor.webhook;


import com.cloudbees.jenkins.plugins.advisor.AdvisorReport;
import com.google.common.base.Optional;
import net.sf.json.JSONObject;
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
      shouldContainParseablePayload(req);
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
     * @throws ServletException  if unparseable
     */
    private void shouldContainParseablePayload(StaplerRequest request) throws ServletException {
        JSONObject json = request.getSubmittedForm();
        request.bindJSON(AdvisorReport.class, json);  
    }

    /**
     * Checks that an incoming request has a valid signature.
     *
     * @param req Incoming request.
     * @throws InvocationTargetException if any of preconditions is not satisfied
     * @throws ServletException  if unparseable
     */
    protected void shouldProvideValidSignature(StaplerRequest req, Object[] args) throws InvocationTargetException, ServletException {
      Optional<String> signHeader = Optional.fromNullable(req.getHeader(SIGNATURE_HEADER));

      if (signHeader.isPresent()) {
        String digest = signHeader.get();
        JSONObject json = req.getSubmittedForm();

        isTrue(
            CalculateValidSignature.setUp(json.toString()).matches(digest),
            String.format("Provided signature [%s] did not match the calculated", digest)
        );
      } else {
          LOGGER.info("Apparently it was null");
          isTrue(false, "Missing header on request");
      }
    }

    /**
     * Utility method to stop preprocessing if condition is false
     *
     * @param condition on false throws exception
     * @param msg       to add to exception
     * @throws InvocationTargetException BAD REQUEST 400 status code with message
     */
    private void isTrue(boolean condition, String msg) throws InvocationTargetException {
      if (!condition) {
        LOGGER.info("Error in Advsior Webhook prechecks: " + msg);
        throw new InvocationTargetException(errorWithoutStack(SC_BAD_REQUEST, msg));
      }
    }
  }
}
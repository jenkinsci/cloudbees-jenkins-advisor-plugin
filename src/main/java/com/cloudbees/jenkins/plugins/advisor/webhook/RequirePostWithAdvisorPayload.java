package com.cloudbees.jenkins.plugins.advisor.webhook;

/**
 * REFERENCE: https://github.com/jenkinsci/github-plugin/blob/405e8536e6d8ce00d92e2a9afe4cd4744756d155/src/main/java/org/jenkinsci/plugins/github/webhook/RequirePostWithGHHookPayload.java
 */

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import javax.servlet.ServletException;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.Interceptor;
import org.kohsuke.stapler.interceptor.InterceptorAnnotation;
import org.slf4j.Logger;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static org.kohsuke.stapler.HttpResponses.error;
import static org.kohsuke.stapler.HttpResponses.errorWithoutStack;
import static org.slf4j.LoggerFactory.getLogger;

@Retention(RUNTIME)
@Target({METHOD, FIELD})
@InterceptorAnnotation(RequirePostWithAdvisorPayload.Processor.class)
public @interface RequirePostWithAdvisorPayload {
  class Processor extends Interceptor {
    private static final Logger LOGGER = getLogger(Processor.class);
    public static final String SIGNATURE_HEADER = "CloudBees-Advisor-Signature";

    @Override
    public Object invoke(StaplerRequest req, StaplerResponse rsp, Object instance, Object[] arguments)
            throws IllegalAccessException, InvocationTargetException, ServletException {
      shouldBePostMethod(req);
      //returnsInstanceIdentityIfLocalUrlTest(req);
      //shouldContainParseablePayload(arguments);
      //shouldProvideValidSignature(req, arguments); //will definitely be revisiting this requirement

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
      LOGGER.info("VALUE OF METHOD: " +request.getMethod());
      if (!request.getMethod().equals("POST")) {
        throw new InvocationTargetException(error(SC_METHOD_NOT_ALLOWED, "Method POST required"));
      }
    }

    /**
     * Checks that an incoming request has a valid signature, if there is specified a signature in the config.
     *
     * @param req Incoming request.
     *
     * @throws InvocationTargetException if any of preconditions is not satisfied
     */
    /*protected void shouldProvideValidSignature(StaplerRequest req, Object[] args) throws InvocationTargetException {
      Optional<String> signHeader = Optional.fromNullable(req.getHeader(SIGNATURE_HEADER));
      Secret secret = GitHubPlugin.configuration().getHookSecretConfig().getHookSecret();

      if (signHeader.isPresent() && Optional.fromNullable(secret).isPresent()) {
        String digest = substringAfter(signHeader.get(), SHA1_PREFIX);
        LOGGER.trace("Trying to verify sign from header {}", signHeader.get());
        isTrue(
                GHWebhookSignature.webhookSignature(payloadFrom(req, args), secret).matches(digest),
                String.format("Provided signature [%s] did not match to calculated", digest)
        );
      }
    }*/
  }
}
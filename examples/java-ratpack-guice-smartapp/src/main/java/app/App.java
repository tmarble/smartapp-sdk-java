package app;

import static ratpack.jackson.Jackson.json;

import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Module;
import com.smartthings.sdk.smartapp.core.SmartApp;
import com.smartthings.sdk.smartapp.core.extensions.HttpVerificationService;
import com.smartthings.sdk.smartapp.core.models.AppLifecycle;
import com.smartthings.sdk.smartapp.core.models.ExecutionRequest;
import com.smartthings.sdk.smartapp.guice.Guice;

import ratpack.config.internal.DefaultConfigDataBuilder;
import ratpack.http.Headers;
import ratpack.http.Request;
import ratpack.http.Status;
import ratpack.server.RatpackServer;


public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String... args) throws Exception {
        HttpVerificationService httpVerificationService = new HttpVerificationService();
        Module appModule = new AppModule();
        SmartApp smartApp = SmartApp.of(Guice.smartapp(bindings -> bindings.module(appModule)));
        RatpackServer.start(server -> {
            ObjectMapper objectMapper = DefaultConfigDataBuilder.newDefaultObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
            server
                .registry(ratpack.guice.Guice.registry(bindingsSpec -> {
                    bindingsSpec.module(appModule);
                    bindingsSpec.bindInstance(ObjectMapper.class, objectMapper);
                }))
                .handlers(chain -> chain
                          .all(new AppRequestLogger(LOG))
                          .get(ctx -> {
                                  LOG.warn("GET---------------");
                                  ctx.getResponse()
                                      .status(Status.FORBIDDEN)
                                      .send("The smart.octet.services app only functions as a SmartThings Automation webhook endpoint");
                                      })
                          .post("smartapp", ctx -> {
                                  LOG.warn("POST--------------");
                                  ctx.parse(ExecutionRequest.class).then(executionRequest -> {
                                          Request request = ctx.getRequest();
                                          Headers headers = request.getHeaders();
                                          Map<String, String> headersMap = headers.getNames().stream()
                                              .collect(Collectors.toMap(name -> name, name -> headers.get(name)));
                                          String method = request.getMethod().getName();
                                          LOG.warn("HEADERS=" + headersMap);
                                          LOG.warn("executionRequest=" + executionRequest);
                                          if (executionRequest.getLifecycle() != AppLifecycle.PING
                                              && !httpVerificationService.verify(method, request.getUri(), headersMap)) {

                                              // ctx.clientError(401);
                                              // } else {
                                              LOG.error("MISSING authentication");
                                          }
                                          ctx.render(json(smartApp.execute(executionRequest)));
                                          // }
                                      });
                              })
                );
            }
        );
    }
}

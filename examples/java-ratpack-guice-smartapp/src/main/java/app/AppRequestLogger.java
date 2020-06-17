package app;

import ratpack.handling.UserId;
import ratpack.handling.RequestId;
import ratpack.handling.RequestLogger;
import ratpack.handling.RequestOutcome;
import ratpack.http.HttpMethod;
import ratpack.http.Request;
import ratpack.http.SentResponse;
import ratpack.http.Status;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.util.Types;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.slf4j.Logger;
import com.google.common.net.HostAndPort;

public class AppRequestLogger implements RequestLogger {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter
			.ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
			.withZone(ZoneId.systemDefault());

	private final Logger logger;

	public AppRequestLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void log(RequestOutcome outcome) {
		if (!logger.isDebugEnabled()) {
			return;
		}

		Request request = outcome.getRequest();
		SentResponse response = outcome.getResponse();
		String responseSize = "-";
		String contentLength = response.getHeaders().get(HttpHeaderConstants.CONTENT_LENGTH);
		if (contentLength != null) {
			responseSize = contentLength;
		}

		StringBuilder logLine = new StringBuilder()
				.append(
						ncsaLogFormat(
								request.getRemoteAddress(),
								"-",
								request.maybeGet(UserId.class).map(Types::cast),
								request.getTimestamp(),
								request.getMethod(),
								request.getRawUri(),
								request.getProtocol(),
								response.getStatus(),
								responseSize));

		request.maybeGet(RequestId.class).ifPresent(id1 -> {
			logLine.append(" id=");
			logLine.append(id1);
		});

		logger.debug(logLine.toString());
	}

	String ncsaLogFormat(HostAndPort client, String rfc1413Ident, Optional<CharSequence> userId, Instant timestamp, HttpMethod method, String uri, String httpProtocol, Status status, String responseSize) {
		return String.format("%s %s %s [%s] \"%s %s %s\" %d %s",
				client.getHost(),
				rfc1413Ident,
				userId.orElse("-"),
				FORMATTER.format(timestamp),
				method.getName(),
				uri,
				httpProtocol,
				status.getCode(),
				responseSize);
	}
}

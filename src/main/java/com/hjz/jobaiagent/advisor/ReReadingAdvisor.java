package com.hjz.jobaiagent.advisor;

import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

/**
 * 自定义 Re2 Advisor
 * 可提高大型语言模型的推理能力
 */
public class ReReadingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

	private AdvisedRequest before(AdvisedRequest advisedRequest) {
		// 直接拼接两遍用户问题 → 真正的 Re2
		String realRe2Text = advisedRequest.userText() + "\nRead the question again: " + advisedRequest.userText();

		return AdvisedRequest.from(advisedRequest)
				.userText(realRe2Text)
				.build();
	}

	@Override
	public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
		return chain.nextAroundCall(this.before(advisedRequest));
	}

	@Override
	public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
		return chain.nextAroundStream(this.before(advisedRequest));
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}
}
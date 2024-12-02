package com.example.nagoyameshi.service;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.nagoyameshi.entity.Contract;
import com.example.nagoyameshi.entity.User;
import com.example.nagoyameshi.form.ReservationRegisterForm;
import com.example.nagoyameshi.repository.ContractRepository;
import com.example.nagoyameshi.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.PriceCollection;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.PriceListParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;

import jakarta.servlet.http.HttpServletRequest;




@Service
public class StripeService {
	private Logger logger = LoggerFactory.getLogger(StripeService.class);

	@Value("${stripe.api-key}")
	private String stripeApiKey;

	private String subscriptionName = "有料テスト-2622925";
	private final ReservationService reservationService;
	private final UserRepository userRepository;
	private final ContractRepository contractRepository;
	public StripeService(
			ReservationService reservationService,
			UserRepository userRepository,
			ContractRepository contractRepository) {
		this.reservationService = reservationService;
		this.userRepository = userRepository;
		this.contractRepository = contractRepository;
	}
	// セッションを作成し、Stripeに必要な情報を返す
	public String createStripeSession(String restaurantName, ReservationRegisterForm reservationRegisterForm,
			HttpServletRequest httpServletRequest) {
		Stripe.apiKey = stripeApiKey;
		String requestUrl = new String(httpServletRequest.getRequestURL());
		SessionCreateParams params = SessionCreateParams.builder()
				.addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
				.addLineItem(
						SessionCreateParams.LineItem.builder()
								.setPriceData(
										SessionCreateParams.LineItem.PriceData.builder()
												.setCurrency("jpy")
												.setUnitAmount(300L) // 月額300円
												.setRecurring(
														SessionCreateParams.LineItem.PriceData.Recurring.builder()
																.setInterval(
																		SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
																.build())
												.setProductData(
														SessionCreateParams.LineItem.PriceData.ProductData.builder()
																.setName("月額有料プラン")
																.build())
												.build())
								.setQuantity(1L)
								.build())
				.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
				.setSuccessUrl(requestUrl.replaceAll("/subscription/create", "") + "/subscription?subscribed")
				.setCancelUrl(requestUrl.replace("/subscription/create", ""))
				.build();
		try {
			Session session = Session.create(params);
			return session.getId();
		} catch (StripeException e) {
			e.printStackTrace();
			return "";
		}
	}
	// セッションから予約情報を取得し、ReservationServiceクラスを介してデータベースに登録する  
	public void processSessionCompleted(Event event) {
		Optional<StripeObject> optionalStripeObject = event.getDataObjectDeserializer().getObject();
		optionalStripeObject.ifPresentOrElse(stripeObject -> {
			Session session = (Session) stripeObject;
			SessionRetrieveParams params = SessionRetrieveParams.builder().addExpand("payment_intent").build();
			try {
				session = Session.retrieve(session.getId(), params, null);
				Map<String, String> paymentIntentObject = session.getPaymentIntentObject().getMetadata();
				reservationService.create(paymentIntentObject);
			} catch (StripeException e) {
				e.printStackTrace();
			}
			System.out.println("予約一覧ページの登録処理が成功しました。");
			System.out.println("Stripe API Version: " + event.getApiVersion());
			System.out.println("stripe-java Version: " + Stripe.VERSION);
		},
				() -> {
					System.out.println("予約一覧ページの登録処理が失敗しました。");
					System.out.println("Stripe API Version: " + event.getApiVersion());
					System.out.println("stripe-java Version: " + Stripe.VERSION);
				});
	}
	// サブスクリプションの登録する
	public Session createSubscription(String email, String successUrl, String cancelUrl) {
		try {
			Stripe.apiKey = stripeApiKey;
			PriceListParams priceParams = PriceListParams.builder()
					.addLookupKey(subscriptionName).build();
			PriceCollection prices = Price.list(priceParams);
			SessionCreateParams params = SessionCreateParams.builder()
					.setCustomerEmail(email)
					.addLineItem(
							SessionCreateParams.LineItem.builder()
									.setPrice(prices.getData().get(0).getId())
									.setQuantity(1L)
									.build())
					.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
					.setSuccessUrl(successUrl)
					.setCancelUrl(cancelUrl)
					.build();
			Session session = Session.create(params);
			return session;
		} catch (StripeException e) {
			throw new RuntimeException(e);
		}
	}
	// サブスクリプションの登録が完了したらデータベースにIDを保持する
	public void processSubscriptionComple(Subscription subscription) {
		logger.info(subscription.toString());
		logger.info(subscription.getId());
		logger.info(subscription.getCustomer());
		var contract = contractRepository.findByCustomerId(subscription.getCustomer());
		if (contract == null) {
			logger.warn("顧客情報が存在しません");
			return;
		}
		contract.setSubscriptionId(subscription.getId());
		contractRepository.save(contract);
	}
	// 顧客の登録が完了したらデータベースにIDを保持する
	public void processCustomerCreated(Customer customer) {
		logger.info(customer.getEmail());
		logger.info(customer.getId());
		//logger.info(subscription.getCustomerObject().toString());
		var user = userRepository.findByEmail(customer.getEmail());
		var contract = contractRepository.findByUser(user);
		if (contract == null) {
			contract = new Contract();
			contract.setUser(user);
		}
		contract.setCustomerId(customer.getId());
		contractRepository.save(contract);
	}
	public Contract getContract(User user) {
		return contractRepository.findByUser(user);
	}
	// サブスクリプションをキャンセルする
	public void deleteSubscription(String email) {
		var user = userRepository.findByEmail(email);
		var contract = contractRepository.findByUser(user);
		try {
			Stripe.apiKey = stripeApiKey;
			Subscription resource = Subscription.retrieve(contract.getSubscriptionId());
			SubscriptionCancelParams params = SubscriptionCancelParams.builder().build();
			Subscription subscription = resource.cancel(params);
			logger.info("cancel: {}", subscription);
		} catch (StripeException e) {
			throw new RuntimeException(e);
		}
	}
	// サブスクリプションのキャンセル完了したらデータベースから削除する
	public void processSubscriptionDelete(Subscription subscription) {
		var contract = contractRepository.findByCustomerId(subscription.getCustomer());
		if (contract == null) {
			logger.warn("顧客情報が存在しません");
			return;
		}
		contractRepository.delete(contract);
	}
	// サブスクリプションで使用しているクレジットカードを変更する
	public Session changeSubscriptionCard(Contract contract, String successUrl, String cancelUrl) {
		try {
			Stripe.apiKey = stripeApiKey;
			SessionCreateParams params = SessionCreateParams.builder()
					.addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
					.setMode(SessionCreateParams.Mode.SETUP)
					.setCustomer(contract.getCustomerId())
					.setSetupIntentData(
							SessionCreateParams.SetupIntentData.builder()
									.putMetadata("customer_id", contract.getCustomerId())
									.putMetadata("subscription_id", contract.getSubscriptionId())
									.build())
					.setSuccessUrl(successUrl)
					.setCancelUrl(cancelUrl)
					.build();
			Session session = Session.create(params);
			return session;
		} catch (StripeException e) {
			throw new RuntimeException(e);
		}
	}
}
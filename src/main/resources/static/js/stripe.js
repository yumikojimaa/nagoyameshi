const stripe = Stripe('pk_test_51QM1eIGCyvxcxC0yvkzR4IDEn1qQJxbIKhoUedaYC5fg4VoEtSDa0b3aMXFs5Q27TIpLpOKfXLSVGVu7JUy2x3dh00AVi6BZTV');
 const paymentButton = document.querySelector('#paymentButton');
 
 paymentButton.addEventListener('click', () => {
   stripe.redirectToCheckout({
     sessionId: sessionId
   })
 });
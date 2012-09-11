
package eu.masconsult.bgbanking.banks;

public class CaptchaException extends Exception {

    private static final long serialVersionUID = -2719216612495157447L;
    private String captchaUri;

    public CaptchaException(String captchaUri) {
        this.captchaUri = captchaUri;
    }

    public String getCaptchaUri() {
        return captchaUri;
    }

}

package bo.bosque.com.impexpap.security.jwt;

public class JwtConfig {
    public static final String RSA_PUBLIC = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1tpvlpDLjelaI4FBW2hC\n" +
            "FfBj0iHejEOOoW4bb0mj/kBf7s2U2ESsY9Tzgol4N1gcCPogU3WfGaNaQ+XK8F0G\n" +
            "/wA9HV2XodXQt4PN46RirU4PBGGCQYXrVWSb+cFJ8uEB8lKqATFvjRvhZWYdjD5t\n" +
            "htmzTBg/malaoT+g0sH2x6sA1MMvEZoZ5SVk0T+C+bMd313eBdqxHZV4hRzXV2QS\n" +
            "sOwtcdEhOBJL1bF47/cyL7quGJsaT7Lm0WbBZg1dKhaJscoge/nBXlc8gWYz+Dvk\n" +
            "F2SHId1QQY6iy+Y9Wm0vKbHcOS50YXAhbOc9do8zFM4XX9UKj0jopMrqaH5nZ0bL\n" +
            "LwIDAQAB\n" +
            "-----END PUBLIC KEY-----";
    public static final String RSA_PRIVATE = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDW2m+WkMuN6Voj\n" +
            "gUFbaEIV8GPSId6MQ46hbhtvSaP+QF/uzZTYRKxj1POCiXg3WBwI+iBTdZ8Zo1pD\n" +
            "5crwXQb/AD0dXZeh1dC3g83jpGKtTg8EYYJBhetVZJv5wUny4QHyUqoBMW+NG+Fl\n" +
            "Zh2MPm2G2bNMGD+ZqVqhP6DSwfbHqwDUwy8RmhnlJWTRP4L5sx3fXd4F2rEdlXiF\n" +
            "HNdXZBKw7C1x0SE4EkvVsXjv9zIvuq4YmxpPsubRZsFmDV0qFomxyiB7+cFeVzyB\n" +
            "ZjP4O+QXZIch3VBBjqLL5j1abS8psdw5LnRhcCFs5z12jzMUzhdf1QqPSOikyupo\n" +
            "fmdnRssvAgMBAAECggEAO/UvaNQ1if/SANCEXa7lqluwb6a2BWyg9BnXHCJv2nc6\n" +
            "jljerc/UT/PQlOAqJT+4ayTqOoA7ixsUCJirHpLHbggyBezlcOtLWLs2jM2GIBkF\n" +
            "hdJ0WDa4Ktdt07AGI/p44ZgCC10xZS6fov1xR37wb853A8hMj2Q8f31TMx2f8a4a\n" +
            "0p6dUjKOuAHuhCnEhwWSZanZ1D2jbYAQRoNCJVJ3jCibtwigYPBcY8xHLQXFOfLq\n" +
            "VQwmDXHrKKVVeoA3/1TIhoKycqtGQQFlUA0RAfQCEo9aAW6LPvJzOrsP9j3vmCb7\n" +
            "s8VvnTtmPtaLYh7So+ZVIi/1+9iu4GP7qrSEwQoGeQKBgQD7Stk7fPJfN54fxZoU\n" +
            "IDyCCxj+pNlGK38JtE0BDyiZtylFl8cY8arcCOlHaFv7YlXy1GMt92TH3/FrTP6u\n" +
            "hBMO9yRUGB/oPcwXyEGXdmaNOyEmWnqwkzKSD9CeW0Vo/TzeEZXB4cP5MvTobsUK\n" +
            "ll2Qg7pUkG52ueFOemYR0RumDQKBgQDa4NUGrQbJjJx38/j12Bqq2F3oPPEEcKyL\n" +
            "h8sjf4cd5jRmJ/HE11oFxEpJVdFoqlBVTy+NI8XjjtbTukVHiQq31ocL588kIr78\n" +
            "VY9B4x2botbufg8JCIWNK3jsbTDnG8clZJ+idlYCirN3YovmlWjpZRGDHgtbKByL\n" +
            "jq+DxT7DKwKBgBUT1R65PzcfWiL+FwtjHNAnkCQjvZm2IkS1G9Rf6h7ijxKoRWnh\n" +
            "M1ybXr2/kh+GwwDIMb10R77AGObQIXiP2W1i62gmUd7P+CNyh5Xlt3pXIFOwOSRA\n" +
            "ZHh93Ri7PRouS4Gw5efKQP7Q+FvalqpprFVnxyQ6rRlGRBCqEY+jA3etAoGBAJDi\n" +
            "v2DZ7GJCV9j3gNeMI6rv6smufYSI6U67pvZqlpBuMEVnL50zSH4Ev3/n9OSpyN+P\n" +
            "uVReV8IYbZBd/zopNxUWRvLUkcPD1FTIdjoREypREqFwhJdgMreODU/Dv9lcA+l2\n" +
            "wE0UtD6efcoLS7xpLrdAleULYE0JMkwXOYuqZ69dAoGANL4KMDVD3Bd/YSj7uPpE\n" +
            "UMw3MQouyXcqpaXBfl2W+WqNyTFVESFSFU3B4tK3Js5rxReIoNc7GuiicziN649c\n" +
            "01twtKkuJgHC9TqdUWZR/LuBFrrt1Ms8uhMMrwsAJTUVkRcbCxmoSfImQrbYd139\n" +
            "T7QlWrkSusa+W6nQKg0aUWs=\n" +
            "-----END PRIVATE KEY-----";
}

package net.payrdr.mobile.payment.sdk.payment.ssl

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** A certificate made for these tests (DER in Base64) and its pin as `openssl ... | openssl dgst -sha256` gives it. */
@OptIn(ExperimentalEncodingApi::class)
internal class TestCertificate(
    val pin: String,
    base64: String,
) {
    val der: ByteArray = Base64.decode(base64.filterNot { it.isWhitespace() })
}

/** Certificates of each key type the pinning supports, and a chain: [LEAF] issued by [CA] for [LEAF_HOST]. */
internal object TestCertificates {
    const val LEAF_HOST = "pinned.example"

    /** RSA 2048, the issuer of [LEAF]. */
    val CA =
        TestCertificate(
            pin = "sha256/N8c35GTwmOHegqkBKrBZ9rMYTvDF5qGfTFw91QkgCbs=",
            base64 =
                """
                MIIDBDCCAeygAwIBAgIUT8c2tpYvK08t27kIFTzOswBI3cYwDQYJKoZIhvcNAQELBQAwGjEYMBYGA1UEAwwPVGVzdCBQaW5uaW5n
                IENBMB4XDTI2MTEwNDIyNTc0N1oXDTM2MTEwMTIyNTc0N1owGjEYMBYGA1UEAwwPVGVzdCBQaW5uaW5nIENBMIIBIjANBgkqhkiG
                9w0BAQEFAAOCAQ8AMIIBCgKCAQEAy3e1Co0vlfvy4va90is7+mIqbOzSXk9W6GSm2PbbLmhftYV7oV/M1sJBP+tWAX9ZFUDz3Abe
                0OXcRBIp96kJY5PtyJ0gnrMnt6NPE8jEV2P/QaWkipBQtWlPQkjNReu2OdUkXcNSIo6cOGseS3z3OQY2coKgeXjHGJaM3RAoKAKG
                WRkzIlzo38Ai66QVG0Us+Ve3QMWUFujETaKTiLCDd/lOvOe/aRHqz0ZfFea5u6K1KBbKu2i2a1roEbi8VOsqG5SKp7yPdtccl91Z
                r7MZ7Ebhi1/4thCLQxyZCqeppaBmBemUMfsMRU46p/QQhPzvE7IHcmwTFURr3KYOs4EurQIDAQABo0IwQDAdBgNVHQ4EFgQUi2sD
                dCOSi8PTekGB2pBMOJPqOKEwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwDQYJKoZIhvcNAQELBQADggEBAG/T24ja
                gt2emCHQewp4sQcgZi96Nno5r/FIdnk0As2LmgNwBgYj4C52kjcnFnlkt0LVYhXwybVeTuagv6fcY8s9R9eCO5Fv6QbAkBqqJK+X
                Laotfa/P7DAkRN2LamQnMjMVG06MFFzY5ISqbu8pXD8DqgeyxdTYAUy7iB6YgS1HCBB6i1Q6DL6q6x8huuF7RM1ZB6beuPaNm7MY
                UB+sUj0uRWHyGKnCZn74iBB2G7clujBuB3alTvFSaP3XQGgYt2/X7eVQyPsSBXpXdZWn1TPkgEnVQIMC98rK8sBO6Q0aGKxON7fY
                Tu+TIiBUvA+4Lu+EvpO0Yg5ku9IhBBDRdhU=
                """,
        )

    /** EC P-256, for [LEAF_HOST], issued by [CA]. */
    val LEAF =
        TestCertificate(
            pin = "sha256/9cax82VpA6cAVbTtcDHCQ+0LpbA1Sc6i0QXQMKwA2UM=",
            base64 =
                """
                MIICQzCCASugAwIBAgIUFyy0qxxvk7UTiF0gMnrKNYV6cNcwDQYJKoZIhvcNAQELBQAwGjEYMBYGA1UEAwwPVGVzdCBQaW5uaW5n
                IENBMB4XDTI2MTEwNDIyNTc0N1oXDTI3MTEwNDIyNTc0N1owGTEXMBUGA1UEAwwOcGlubmVkLmV4YW1wbGUwWTATBgcqhkjOPQIB
                BggqhkjOPQMBBwNCAARU9mSFvvZ6LPqkvdvOOjmqTvNK5rwuloeN0/INvtDyYiw4RPZNKHSYVXwHPB8ahHb7uPRa4l4mnNJ+Bu25
                R/yQo00wSzAJBgNVHRMEAjAAMA4GA1UdDwEB/wQEAwIHgDATBgNVHSUEDDAKBggrBgEFBQcDATAZBgNVHREEEjAQgg5waW5uZWQu
                ZXhhbXBsZTANBgkqhkiG9w0BAQsFAAOCAQEAegV5o9prlvnkaNrGF0lte51huexfYjDRMZTm4iRbx8raYg+UDcEGTbe2FeUVttLb
                T/tiguPduBgBwuKHEOp30hW66S+bXByq+I93eLOAFTVn/rsNmqp7F0ZGQGro4rhlalC0xiBP4zEmc1z8lff2TGZB0O4zvTuiSSI5
                3GIPqvfnGv0Xacle+P6+wh8umszBry+bPfHBi/WIH1+tkaZjc/zkihZCohNHysEvOmrOEvjyqZjhV+u+//v5z4pU0jBvjr+PZ1eR
                AS5gpXY5TppQByhPozv0zlsJuyIwsKM520T4r2OMa+WFG2MCbyidwoqa5fuxb+THeQdSaQyLil4GIw==
                """,
        )

    /** RSA 3072, self-signed. */
    val RSA_3072 =
        TestCertificate(
            pin = "sha256/SVSKUNOcMWyzpXAnSXtbFq3d54ld++dqdDdgKUUrhTg=",
            base64 =
                """
                MIID5DCCAkygAwIBAgIUXBvlaHfDXh4gJpHuwIMjwzvumxMwDQYJKoZIhvcNAQELBQAwEjEQMA4GA1UEAwwHcnNhMzA3MjAeFw0y
                NjExMDQyMjU3NDdaFw0yNzExMDQyMjU3NDdaMBIxEDAOBgNVBAMMB3JzYTMwNzIwggGiMA0GCSqGSIb3DQEBAQUAA4IBjwAwggGK
                AoIBgQDiN4LeBDB0H8ArrE0YRDCGC0RMzTTPE19FhB4C3rWmnWUnhh9h1ZQfaMBYo+ZcuMl0PSJd2shKnU42mVUyQ47yDcf6E33K
                F/mO+p1b+TZFCzULbSuG/RL74j7cujjb+El/MUhn7nVAZ7dQ53oTIzlxW34l98Xkkx4qIJjeFt9HqqrFNKi4mr2XDA5Rqk72kFuu
                nB8zd9oU3bizr5qYDwk+psEA4iRF5F3yI1v1neiKxonn/mNAg+1o2iS0pPhip36YTSBhxh+0D1IDcyLEDbi2f/ns8tQHJ/IOjVCD
                hG1K5UqYRNO2fNp1g6hQVke7zKscVvZiu1M8APCVk8TZCZhltWXqFi7aHbhYdocTQCUTx8S+XNM+/n1CJXi69Nu7cxzxjmAtSXRA
                x99JV5RkGYRMycS6G8OncBvnsGXFpXjNB3S2Lqtxw567gBSzm6hSRjLPVA8VDMyRfmj92hgTNpYbeiMevuy0P7lMk+0X7RWgD+bW
                SyudUivcGOy4ygGo0pUCAwEAAaMyMDAwHQYDVR0OBBYEFJvYqa9ZienEfat/cou7H9WSiCCoMA8GA1UdEwEB/wQFMAMBAf8wDQYJ
                KoZIhvcNAQELBQADggGBAJ0ps6yy2559kZOSnBHjLJQNp07nOTfUai4sKwXIuPVNdf0KcQ2eDq+eUB3sAdEypz5djQgDrnHKvQFA
                aWEaNd4ucHP/FyFWW3mQY3Opt3IJKksh0RMNVoWb16rso7SrbVKAKyYm5SHJ0ODj3+EOUOD/cJ4q4NGyv6ajkUHriek6J86hpzv8
                JnAD43dbm/WE/qvdesX6Zd4WtyRrGXmkquSVTgwfaNiEok3TXtAjWDX8205CO9ReSNMnrGIku0lXMoNXJk58osgFhbScAMcpy+p8
                JKvAruHabh+99gOBbSJuOyCtEcnu5BjL2g3YZLntCx1s5hkhIrjJ+dgcvG7h+42LQKlj+qI65ONiuPP1oDOmZHjFnvvNGgeN1vRV
                xZXzRoKS066kNDhgNrY+AjRp9q4zKxzMnt4fxnAvg8KDnpaQii9JC3BD2N8H+GkxKlMo/DU6sffsGI1OeF9gxvOviJk1seECLcZZ
                Zq51xjMQWg+6BPAcsamY2ouK4z8bWR6Jcw==
                """,
        )

    /** RSA 4096, self-signed. */
    val RSA_4096 =
        TestCertificate(
            pin = "sha256/vIiWgDfcJ4ANhW/RLQJnQANblP5qUe6PIamGwJ8tLsg=",
            base64 =
                """
                MIIE5DCCAsygAwIBAgIUPydGvPzX8KmBvjk7RTg4lpp9Yj8wDQYJKoZIhvcNAQELBQAwEjEQMA4GA1UEAwwHcnNhNDA5NjAeFw0y
                NjExMDQyMjU3NDhaFw0yNzExMDQyMjU3NDhaMBIxEDAOBgNVBAMMB3JzYTQwOTYwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIK
                AoICAQDAs6Cg0VI9mKt3223x1g3JX1BJG4rOMyGMpPHZGS2FFob4OhmKHwI5pPVq/fFbT3sKMN6XVlJaD4Yutwjk9ITiuUVQTf31
                l1NiUBaAZoUzznVg/hfuKzEaBdlDt8z4WGyukn7yfyBi0P1tgEN2FArZSsQeCHv/ulQE6srJdWFYc8wBGKCtd5yXDFIlwuC5QZhT
                yIAS1/Dy95hVJMVQJg8ebU9c8wzfAZ65iM3GwCUfKne8JJxyao96k6nN+SGbFiz3Fib+Y/ovN8akY2+tXPBXnyrsLCd4RCwR7e9n
                ESTfQeyCd/F7J1gdYR30fg8mc4kOC5BFIrwMrdK27jYmhMVHStugyDH9tUCOuxkWdzuunIH8+U4GSzyC3D4gBMfzaoBjTSNnnrbb
                fwwOH1DRWKr+rPcSErzoJ8inSVhvF2djMC9OlD/z85eKd92NcKFcThpJ9jeXVHmari/ZVlO/xn2/iZrOEsFuuY5jjrIrjvtqHuLE
                1ivfNNRd6746uzWPKLkUYGPNgvDy1NqRX6w3kY3YzjkqDw0CTpuBFd94Aud2xgk3O5vdE7y4tONnaIglSf0fOlMPlUniS3AwfYLL
                VdW34eXastfDoV75l59UT2Rc86vGmWNIJrkwerCbLCNCncp6qRUNGu8eKO69Mw4zO7EmOzS89yIzgHcpaN+EI2W63QIDAQABozIw
                MDAdBgNVHQ4EFgQUxhzcet5t/AD1Lw2kohovwjttTWEwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAgEAEwTC/JUe
                PIUrqW1LjblEVEttjfnSceVI7fM3dUd7D72t05T8VSzSq2ppF0OLrQ/yrKD/xrOYE6gomM/jATR3KGu+dDV4TR+G6PL6H/JQVCGr
                PiKNqlfM/dovLivaW0gjz8/+FeESI7zJdMCH9Z+n6eZOl/hWymracVd/wvDFbWEvjmCYlPBkzZY9z7o9YajhBrNWEoICuoqdwTMl
                9871qfWZGjDt6TWO25Z3gQjdjnIACBhZmR/oMOgMNmiQyT+xeECzZg0hOwFAC/4kSqUtl/pSboKIH1S+k6kk+Rj0CZj9ncuPwcT5
                rUKw3fwB3CI4cdG6l1LC1tmze+SsrjXa9SJ9npxW/fvT8z3ltsgWG7OJ0NJxfegbCc1Xe8hMFQZMyNCxrNF19HLgE72O+/69YBJa
                reyo84T6njo1xUiDGSFIiNozBsbTa4BqdBUAYebgYcCzbYecJfHXsVBtWATiMtTRlCgIqleGdOGomRPtNUGINJtF63M1k0GvSXXW
                dnDYQBOSuXtnac2zofK+i3wI3BtIXYG9JSs/C7OJtiVOZ1ihjlaTGRJnWef8z0i++O48tQY/AoBatj3sIYiSlpWiLetS3zL9f+N/
                BVtTjgjqwGzy1zHnfYJ5vdEzvJfZeCGcymUwfv5MZZHSnuB5tODPF7PghTcJ1MkKJNad6cobxlo=
                """,
        )

    /** EC P-384, self-signed. */
    val EC_P384 =
        TestCertificate(
            pin = "sha256/KqHooL3D62k74WCKEPMdBDxth2no8v2d4OVYYrXOnoA=",
            base64 =
                """
                MIIBkjCCARegAwIBAgIUAgpZqWvWYvI0JS4Ko3XCk2z9/kEwCgYIKoZIzj0EAwIwEDEOMAwGA1UEAwwFZWMzODQwHhcNMjYxMTA0
                MjI1NzQ4WhcNMjcxMTA0MjI1NzQ4WjAQMQ4wDAYDVQQDDAVlYzM4NDB2MBAGByqGSM49AgEGBSuBBAAiA2IABICIXS+6rp545Kp+
                pGMJ4ftLBsIunihZUrgoXmZW5hT+iOnqm6k8vO5WGGCLH40hvyXbiPl3DaASvHDPMifzYYLOClPF3I1z64feXAv6Gq0vx5N57lgU
                a00EQF05J7jL66MyMDAwHQYDVR0OBBYEFFe8bWwdEk90Mr59sxZlADqkNfV0MA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwID
                aQAwZgIxAJjmGFdy+mdHrw+UOuf5GYRtD2J+FssXK1ZxQxcy6rrzdOlblZUM6/T3Q3J23iUORQIxANW3S4bmi686xcUYBa4RSqLk
                lbZV7ktLQQ9B7RVH/dm8ttlr04jgsi40oEhZ52vNoA==
                """,
        )
}

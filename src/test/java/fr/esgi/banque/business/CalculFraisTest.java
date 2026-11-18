package fr.esgi.banque.business;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CalculFraisTest {

    private final CalculFrais calcul = new CalculFrais();

    @Nested
    @DisplayName("F1 : titulire majeur et solde moyen obligatoire")
    class F1_Validation {

        @ParameterizedTest(name = "age {0} refusé")
        @ValueSource(ints = {17, 0, -1})
        void titulaireMineureRefuse(int age) {
            BigDecimal soldeMoyen = new BigDecimal("500");

            assertThatThrownBy(() -> calcul.fraisMensuels(age, soldeMoyen, false))
            .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void titulaireDe18AnsAccepte() {
            BigDecimal soldeMoyen = new BigDecimal("500");

            assertThatNoException().isThrownBy(() -> calcul.fraisMensuels(18, soldeMoyen, false));
        }

        @Test
        void soldeMoyenNullRefuse() {
            assertThatThrownBy(() -> calcul.fraisMensuels(30, null, false))
            .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("F2 : frais de tenu de 2,00 euros par mois")
    class F2_FraisDeTenue {

        @ParameterizedTest(name = "age {0}, solde moyen {1}: 2,00 euros")
        @CsvSource({
            "27, 0",
            "30, 500",
            "65, 999.99"
        })
        void fraisDeTenueDe2Euros(int age, BigDecimal soldeMoyen) {
            BigDecimal frais = calcul.fraisMensuels(age, soldeMoyen, false);

            assertThat(frais).isEqualByComparingTo("2.00");
        }
    }

    @Nested
    @DisplayName("F3 : tenue offerte aux moins de 26 an")
    class F3_Jeunes {

        @ParameterizedTest(name = "age {0}, solde moyen {1} : tenue offerte")
        @CsvSource({
            "18, 0",
            "20, 500",
            "25, 999.99"
        })
        void moinsDe26AnsTenueOfferte(int age, BigDecimal soldeMoyen) {
            BigDecimal frais = calcul.fraisMensuels(age, soldeMoyen, false);

            assertThat(frais).isEqualByComparingTo("0");
        }

        @ParameterizedTest(name = "age {0} : tenue due")
        @ValueSource(ints = {26, 27})
        void aPartirDe26AnsTenueDue(int age) {
            BigDecimal soldeMoyen = new BigDecimal("500");

            BigDecimal frais = calcul.fraisMensuels(age, soldeMoyen, false);

            assertThat(frais).isEqualByComparingTo("2.00");
        }
    }

    @Nested
    @DisplayName("F4 : tenue offerte si le solde moyen atteint 1 000 Euros")
    class F4_SoldeMoyen {

        @ParameterizedTest(name = "solde moyen {0} : tenue offerte ")
        @ValueSource(strings = {"1000", "1000.00", "1000.01", "5000"})
        void soldeMoyenDe1000OuPlusTenueOfferte(BigDecimal soldeMoyen) {
            BigDecimal frais = calcul.fraisMensuels(40, soldeMoyen, false);

            assertThat(frais).isEqualByComparingTo("0");
        }

        @ParameterizedTest(name = "solde moyen {0} : tenue due")
        @ValueSource(strings = {"999.99", "0", "-100"})
        void soldeMoyenSous1000TenueDue(BigDecimal soldeMoyen) {
            BigDecimal frais = calcul.fraisMensuels(40, soldeMoyen, false);

            assertThat(frais).isEqualByComparingTo("2.00");
        }
    }

    @Nested
    @DisplayName("F5 : la carte prenium ajoute 5,00 euros toujours dus")
    class F5_CartesPrenium {

        @ParameterizedTest(name = "age {0], solde moyen {1}, prenium : {2} euros ")
        @CsvSource({
            "40, 500, 7.00",
            "26, 999.99, 7.00",
            "20, 500, 5.00",
            "25, 999.99, 5.00",
            "40, 1000, 5.00",
            "20, 5000, 5.00"
        })
        void supplementPreniumToujoursDu(int age, BigDecimal soldeMoyen, BigDecimal fraisAttendus) {
            BigDecimal frais = calcul.fraisMensuels(age, soldeMoyen, true);

            assertThat(frais).isEqualByComparingTo(fraisAttendus);
        }
    }







}
package fr.esgi.banque.business;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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




}
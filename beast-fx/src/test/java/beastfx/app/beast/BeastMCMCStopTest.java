package beastfx.app.beast;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Stop clicked in the BEAST dialog while the file is still being parsed (#329 review):
 * the request is kept, and run() doesn't start the analysis.
 */
class BeastMCMCStopTest {

    @Test
    void stopBeforeTheAnalysisExistsIsKept() throws Exception {
        BeastMCMC beastMCMC = new BeastMCMC();
        assertFalse(beastMCMC.hasAnalysis());
        assertTrue(beastMCMC.requestStop(), "a stop while parsing is accepted, not refused");
        beastMCMC.run(); // returns without running anything (m_runnable is still null)
    }
}

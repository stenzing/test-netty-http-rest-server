package sg.sample.bl;

import sg.sample.model.*;

public interface TransferService {

    /**
     * @param request
     * @return
     */
    TopupResponse process(TopupRequest request);

    /**
     * Initiate a transfer between two users
     *
     * @param request the transfer request
     * @return the response
     */
    TransferResponse process(TransferRequest request);

    /**
     * Check the balance of a given user
     *
     * @param request the balance request
     * @return the response
     */
    BalanceResponse process(BalanceRequest request);
}

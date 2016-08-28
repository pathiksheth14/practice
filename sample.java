public string GetFundamentalTicker(string identifier, InstrumentType identifierType = InstrumentType.CUSIP)
        {
            PerSecurityWS ps = new PerSecurityWS();
            try
            {


                log.DebugFormat("Cert path is: {0}", CertPath);                
                X509Certificate2 clientCert = new X509Certificate2(CertPath, "<password_redacted>");

                ps.ClientCertificates.Add(clientCert);
            }
            catch (Exception e)
            {
                log.ErrorFormat("Error in cert setup - {0} - {1}", e.Message, e.InnerException == null ? "" : e.InnerException.Message);
                return null;
            }
            //Set request header
            GetDataHeaders getDataHeaders = new GetDataHeaders();
            getDataHeaders.secmaster = true;
            getDataHeaders.secmasterSpecified = true;
            //getDataHeaders.fundamentals = true;
            //getDataHeaders.fundamentalsSpecified = true;
            //getDataHeaders.programflag = ProgramFlag.oneshot;//unnecessary - defaults to this anyway
            //getDataHeaders.programflagSpecified = true;
            //getDataHeaders.pricing = true;
            getDataHeaders.secid = identifierType;
            getDataHeaders.secidSpecified = true;                    

            SubmitGetDataRequest sbmtGtDtreq = new SubmitGetDataRequest();
            sbmtGtDtreq.headers = getDataHeaders;

            sbmtGtDtreq.fields = new string[] { 
                                                "DEBT_TO_EQUITY_FUNDAMENTALS_TKR"                                                
            };

            int currentFundYear = DateTime.Now.Year;

            //var fundYears = new List<int>();



            List<Instrument> fundYearInstruments = new List<Instrument>();

            Instrument fundYearInstrument = null;

            fundYearInstrument = new Instrument();
            fundYearInstrument.id = identifier;
            fundYearInstrument.typeSpecified = true;
            fundYearInstrument.type = identifierType;

            fundYearInstrument.yellowkey = MarketSector.Corp;
            fundYearInstrument.yellowkeySpecified = true;
            //fundYearInstrument.overrides = new Override[] {};//{ new Override() { field = "EQY_FUND_YEAR", value = currentFundYear.ToString() } };
            fundYearInstruments.Add(fundYearInstrument);
            //fundYears.Add(-1);



            Instrument[] instr = fundYearInstruments.ToArray();
            Instruments instrs = new Instruments();
            instrs.instrument = instr;

            sbmtGtDtreq.instruments = instrs;



            try
            {


                SubmitGetDataResponse sbmtGtDtResp = ps.submitGetDataRequest(sbmtGtDtreq);                       


                RetrieveGetDataRequest rtrvGtDrReq = new RetrieveGetDataRequest();
                rtrvGtDrReq.responseId = sbmtGtDtResp.responseId;

                RetrieveGetDataResponse rtrvGtDrResp;

                do
                {
                    System.Threading.Thread.Sleep(POLL_INTERVAL);
                    rtrvGtDrResp = ps.retrieveGetDataResponse(rtrvGtDrReq);
                }
                while (rtrvGtDrResp.statusCode.code == DATA_NOT_AVAILABLE);                       



                if (rtrvGtDrResp.statusCode.code == SUCCESS)
                {

                    for (int i = 0; i < rtrvGtDrResp.instrumentDatas.Length; i++)
                    {

                        for (int j = 0; j < rtrvGtDrResp.instrumentDatas[i].data.Length; j++)
                        {

                            if (rtrvGtDrResp.instrumentDatas[i].data[j].value == "N.A." || rtrvGtDrResp.instrumentDatas[i].data[j].value == "N.S." || rtrvGtDrResp.instrumentDatas[i].data[j].value == "N.D.")
                                rtrvGtDrResp.instrumentDatas[i].data[j].value = null;

                            return rtrvGtDrResp.instrumentDatas[i].data[j].value;

                        }


                    }
                    return null;
                }
                else if (rtrvGtDrResp.statusCode.code == REQUEST_ERROR)
                {
                    log.ErrorFormat("Error in the submitted request: {0}", rtrvGtDrResp.statusCode.description);
                    return null;
                }
            }
            catch (Exception e)
            {
                log.ErrorFormat("Error in GetData - {0} - {1}", e.Message, e.InnerException == null ? "" : e.InnerException.Message);
                return null;
            }

            return null;
        }

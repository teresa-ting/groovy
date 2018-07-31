package com.lanmao.testsuite.incomeTransfer

import com.lanmao.autotest.XiamenCunguanSpecification

/**
 * Created by yeting on 2017/8/4.
 * 2.0 散标交易(异步) （收入账户测试用例） 共0。1元
 */
class newAsyTransactionIncomeTest extends XiamenCunguanSpecification {

    def investor001
    def borrower001
    def investor002
    def guaranteecorp001
    static def projectNo                     // 标的编号
    static def projectAmount = 10            // 标的金额
    static def preMarketingAmount            // 红包
    def profitAmount                         // 分润金额
    static def amount
    static def requestNO                     // 预处理请求流水号
    static def saleRequestNO                 // 债权出让请求流水
    static def freezeRequestNO               // 追加冻结请求流水号
    static def saleShare                     // 债权出让份额
    def incomeSystemUser
    def password
    def switchValue
    def incomeUser = 'SYS_GENERATE_004'

    def setup() {
        switchValue = configAll().contextSwitch.value
        investor001 = configAll()[switchValue].userInfor.investor001
        investor002 = configAll()[switchValue].userInfor.investor002
        borrower001 = configAll()[switchValue].userInfor.borrower001
        guaranteecorp001 = configAll()[switchValue].userInfor.guaranteecorp001
        password = configAll()[switchValue].userInfor.password
        incomeSystemUser = configAll()[switchValue].userInfor.incomeSystemUser


    }

    def '创建标的'() {

        /**
         * 前置:
         * 1. 存在借款人borrower001
         */
        def userInfo1 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])
        /**
         * 操作:
         * 创建标的号，发送报文
         *
         * 期望结果:
         * 1. 操作成功
         */

        when:
        projectNo = "projectNO" + uuid()
        sleep(1000)
        def requestNo = uuid()
        def req = [
                platformUserNo     : borrower001,
                requestNo          : requestNo,
                projectNo          : projectNo,
                projectType        : "STANDARDPOWDER",
                projectAmount      : projectAmount,
                projectName        : "标的名称-自动化测试",
                annnualInterestRate: 0.01,
                projectPeriod      : 90,
                repaymentWay       : "FIXED_BASIS_MORTGAGE",
        ]
        def resp = direct('ESTABLISH_PROJECT', req)
        def projectInfo = direct("QUERY_PROJECT_INFORMATION", [
                projectNo: projectNo
        ])

        then:
        eq resp.code, '0'
        eq projectInfo.projectNo, projectNo
    }

    def '投标预处理移动端'() {

        /**
         * 前置:
         * 1. 存在投资人investor001, 且帐户可用余额充足
         */
        def userInfo1 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])

        /**
         * 操作:
         *  发送报文，投标金额0.05, 红包金额0.01
         *
         * 期望结果:
         * 1. 跳转到输入交易密码页面
         */
        when:
        amount = 0.05
        preMarketingAmount = 0.01
        requestNO = uuid()
        def req = [
                platformUserNo    : investor001,
                requestNo         : requestNO,
                bizType           : "TENDER",
                amount            : amount,
                preMarketingAmount: preMarketingAmount,
                projectNo         : projectNo,
                expired           : "20281210120000"
        ]
        gateway('USER_PRE_TRANSACTION', req, "MOBILE")

        then:
        $("#password").size() == 1

        /**
         * 操作:
         *  输入交易密码，点击确定
         *
         * 期望结果:
         * 1. 操作成功，冻结资金增加0.05
         */
        when:
        log "输入密码并点击按钮"
        $("#password").val(password)
        $('#nextButton').click()
        def resp = response()
        def userInfo2 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])

        then:
        eq resp.code, 0
        eq new BigDecimal(userInfo2.freezeAmount.toString()).subtract(new BigDecimal(userInfo1.freezeAmount.toString())), amount
    }

    def '异步接口散标放款确认1'() {

        /**
         * 前置:
         * 1. 存在借款人borrower001, 投资人investe001, 分润方invester002 营销款账户SYS_GENERATE_002
         */
        def borrowerInfoB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])
        def investorInfo01B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])
        def investorInfo02B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])
        def sys02B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: "SYS_GENERATE_002"
        ])
        def incomeUserB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])

        /**
         * 操作:
         *  发送报文，放款金额0.05, 红包金额0.01,分润investor002 0.01
         *           佣金：借款人、投资人0.01
         */
        when:
        def requestNo = uuid()
        def req = [
                "batchNo": "cgt_batchno150234r5880902", "bizDetails": [
                [
                        requestNo: requestNo,
                        projectNo: projectNo,
                        tradeType: "TENDER",
                        details  : [
                                [
                                        bizType             : "TENDER",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: investor001,
                                        targetPlatformUserNo: borrower001,
                                        amount              : amount - 0.02,
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share: '1'

                                ],
                                [
                                        bizType             : "MARKETING",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: "SYS_GENERATE_002",
                                        targetPlatformUserNo: borrower001,
                                        amount              : "0.01",
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share: '1'
                                ],
                                [
                                        bizType             : "COMMISSION",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: investor001,
                                        targetPlatformUserNo: incomeUser,
                                        amount              : "0.01",
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share: '1',
                                ],
                                [
                                        bizType             : "COMMISSION",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: borrower001,
                                        targetPlatformUserNo: incomeUser,
                                        amount              : "0.01",
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share: '1',
                                ],
                                [
                                        bizType             : "PROFIT",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: borrower001,
                                        targetPlatformUserNo: investor002,
                                        amount             : "0.01",
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share: '1'
                                ]
                        ]

                ]
        ]
        ]



        def resp = direct('ASYNC_TRANSACTION', req)


        for (int i = 0; i < 100; i++) {
            def result = direct("QUERY_TRANSACTION", [
                    requestNo      : requestNo,
                    transactionType: "TRANSACTION",
            ])
            if (result.records.status[0].equals("SUCCESS")) {
                println("订单状态成功")
                break;
            }
            sleep(2000)
        }

        then:

        def borrowerInfoA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])
        def investorInfo01A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])
        def investorInfo02A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])
        def sys02A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: "SYS_GENERATE_002"
        ])
        def incomeUserA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])

        /**
         * 期望结果:
         * 1. 操作成功，
         *    投资人investor001冻结金额减少0.04
         *    分润方investor002账户余额增加0.01
         *    借款人borrower001账户余额增加0.02
         *    营销款SYS_GENERATE_002账户余额减少0.01
         *    收入账户余额增加0.02
         *
         *
         */

        eq resp.code, 0
        eq new BigDecimal(investorInfo01B.freezeAmount.toString()).subtract(new BigDecimal(investorInfo01A.freezeAmount.toString())), amount - 0.01
        eq new BigDecimal(investorInfo02A.balance.toString()).subtract(new BigDecimal(investorInfo02B.balance.toString())), 0.01
        eq new BigDecimal(borrowerInfoA.balance.toString()).subtract(new BigDecimal(borrowerInfoB.balance.toString())), 0.02
        eq new BigDecimal(sys02B.balance.toString()).subtract(new BigDecimal(sys02A.balance.toString())), 0.01
        eq new BigDecimal(incomeUserA.balance.toString()).subtract(new BigDecimal(incomeUserB.balance.toString())), 0.02


    }

    def '异步接口散标放款确认2'() {

        /**
         * 前置:
         * 1. 存在借款人borrower001, 投资人investe001
         */
        def borrowerInfoB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])
        def investorInfo01B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])



        /*
         * 操作:
         *  发送报文，放款金额0.01
         */

        when:
        def requestNo = uuid()
        def req = [
                "batchNo": "cgt_batchno150234r5880902", "bizDetails": [
                [
                        requestNo: requestNo,
                        projectNo: projectNo,
                        tradeType: "TENDER",
                        details  : [
                                [
                                        bizType             : "TENDER",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: investor001,
                                        targetPlatformUserNo: borrower001,
                                        amount              : "0.01",
                                        customDefine        : "2016102100001"
                                ]
                        ]
                ]
        ]
        ]

        def resp = direct('ASYNC_TRANSACTION', req)

        for (int i = 0; i < 100; i++) {
            def result = direct("QUERY_TRANSACTION", [
                    requestNo      : requestNo,
                    transactionType: "TRANSACTION",
            ])
            if (result.records.status[0].equals("SUCCESS")) {
                println("订单状态成功")
                break;
            }
            sleep(2000)
        }

        def borrowerInfoA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])
        def investorInfo01A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])

        then:

        /**
         * 期望结果:
         * 1. 操作成功
         * 2. 借款人账户增加0.01 投资人账户冻结资金减少0.01
         */
        eq resp.code, 0
        eq new BigDecimal(investorInfo01B.freezeAmount.toString()).subtract(new BigDecimal(investorInfo01A.freezeAmount.toString())), 0.01
        eq new BigDecimal(borrowerInfoA.balance.toString()).subtract(new BigDecimal(borrowerInfoB.balance.toString())), 0.01


    }

    def '债权出让'() {
        /**
         * 前置:
         * 1. 存在债权出让人investor001、购买人investor002
         * 2. 查询标的已投金额
         *
         */
        def proInfor = direct("QUERY_PROJECT_INFORMATION", [
                projectNo: projectNo
        ])

        /**
         * 操作:
         * 发送报文
         *
         * 期望结果:
         * 1. 操作成功
         */

        when:
        saleRequestNO = uuid()
        saleShare = proInfor.loanAmount
        def req = [
                platformUserNo: investor001,
                requestNo     : saleRequestNO,
                projectNo     : projectNo,
                saleShare     : saleShare,

        ]
        def resp = direct('DEBENTURE_SALE', req)
        def projectInfo = direct("QUERY_PROJECT_INFORMATION", [
                projectNo: projectNo
        ])


        then:
        eq resp.code, '0'
        eq projectInfo.projectNo, projectNo
    }

    def '债权竞拍预处理(不锁定份额)'() {

        /**
         * 前置:
         * 1. 存在购买债权人investor002, 且帐户可用余额充足
         */
        def userInfo1 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])

        /**
         * 操作:
         *  发送报文，购买份额0.05, 红包金额0.01
         *
         * 期望结果:
         * 1. 跳转到输入交易密码页面
         */
        when:
        amount = saleShare - 0.01
        preMarketingAmount = 0.01
        requestNO = uuid()
        freezeRequestNO = requestNO
        def req = [
                platformUserNo     : investor002,
                requestNo          : requestNO,
                bizType            : "CREDIT_ASSIGNMENT",
                amount             : amount,
                preMarketingAmount : preMarketingAmount,
                creditsaleRequestNo: saleRequestNO,
                projectNo          : projectNo,
                share              : saleShare,
                shareLock          : "UNLOCK",
                expired            : "20281210120000"
        ]
        gateway('USER_PRE_TRANSACTION', req)
        sleep(7000)

        then:
        $("#password").size() == 1

        /**
         * 操作:
         *  输入交易密码，点击确定
         *
         * 期望结果:
         * 1. 操作成功，冻结资金增加0.03
         */
        when:
        log "输入密码并点击按钮"
        $("#password").val(password)
        $('#nextButton').click()
        sleep(3000)
        def resp = response()
        def userInfo2 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])

        then:
        sleep(2000)
        eq resp.code, 0
        eq new BigDecimal(userInfo2.freezeAmount.toString()).subtract(new BigDecimal(userInfo1.freezeAmount.toString())), amount

    }

    def '债权转让预处理'() {

        /**
         * 前置:
         * 1. 存在购买债权人investor002, 且帐户可用余额充足
         */
        def userInfo1 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])

        /**
         * 操作:
         *  发送报文，购买份额0.05, 红包金额0.01
         *
         * 期望结果:
         * 1. 跳转到输入交易密码页面
         */
        when:
        amount = saleShare - 0.01
        preMarketingAmount = 0.01
        requestNO = uuid()
        freezeRequestNO = requestNO
        def req = [
                platformUserNo     : investor002,
                requestNo          : requestNO,
                bizType            : "CREDIT_ASSIGNMENT",
                amount             : amount,
                preMarketingAmount : preMarketingAmount,
                creditsaleRequestNo: saleRequestNO,
                projectNo          : projectNo,
                share              : saleShare,
                expired            : "20281210120000"
        ]
        gateway('USER_PRE_TRANSACTION', req)
        sleep(2000)

        then:
        $("#password").size() == 1

        /**
         * 操作:
         *  输入交易密码，点击确定
         *
         * 期望结果:
         * 1. 操作成功，冻结资金增加0.03
         */
        when:
        log "输入密码并点击按钮"
        $("#password").val(password)
        $('#nextButton').click()
        sleep(5000)
        def resp = response()
        def userInfo2 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])

        then:
        sleep(2000)
        eq resp.code, 0
        eq new BigDecimal(userInfo2.freezeAmount.toString()).subtract(new BigDecimal(userInfo1.freezeAmount.toString())), amount
    }

    def '债权转让确认有分润'() {
        /**
         * 前置:
         * 1. 存在受让人investe002, 出让人investe001, 营销款账户SYS_GENERATE_002
         */
        def investorInfo01B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])
        def investorInfo02B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])
        def sys02B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: "SYS_GENERATE_002"
        ])
        def incomeUserB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])


        /**
         * 操作:
         *  发送报文, 债转金额0.03, 红包金额0.01,
         *           收取出让人佣金0.01, 受让人0.01
         *           出让人分润给分润方guaranteecorp001 0.01
         *
         * 期望结果:
         * 1. 操作成功
         */
        when:
        def requestNo = uuid()
        def req = [
                "batchNo": "cgt_batchno150234r5880902", "bizDetails": [
                [
                        requestNo    : requestNo,
                        projectNo    : projectNo,
                        tradeType    : "CREDIT_ASSIGNMENT",
                        saleRequestNo: saleRequestNO,
                        details      : [
                                [
                                        bizType             : "CREDIT_ASSIGNMENT",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: investor002,
                                        targetPlatformUserNo: investor001,
                                        amount              : amount - 0.01,
                                        share               : saleShare,
                                        customDefine        : "2016102100001",
                                        income: '1',

                                ],
                                [
                                        bizType             : "MARKETING",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: "SYS_GENERATE_002",
                                        targetPlatformUserNo: investor001,
                                        amount              : "0.01",
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1",
                                ],
                                [
                                        bizType             : "COMMISSION",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: investor001,
                                        targetPlatformUserNo: incomeUser,
                                        amount              : "0.01",
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1",
                                ],
                                [
                                        bizType             : "COMMISSION",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: investor002,
                                        targetPlatformUserNo: incomeUser,
                                        amount              : "0.01",
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1",
                                ],
                                [
                                        bizType             : "PROFIT",
                                        sourcePlatformUserNo: investor001,          // 流水号不传，传了报错：账户冻结资金不足
                                        targetPlatformUserNo: guaranteecorp001,
                                        amount              : "0.01",
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1",
                                ]

                        ]

                ]
        ]
        ]

        def resp = direct('ASYNC_TRANSACTION', req)

        for (int i = 0; i < 100; i++) {
            def result = direct("QUERY_TRANSACTION", [
                    requestNo      : requestNo,
                    transactionType: "TRANSACTION",
            ])
            if (result.records.status[0].equals("SUCCESS")) {
                println("订单状态成功")
                break;
            }
            sleep(2000)
        }

        then:

        def investorInfo01A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])
        def investorInfo02A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])
        def sys02A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: "SYS_GENERATE_002"
        ])
        def incomeUserA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])


        /**
         * 期望结果:
         * 1. 操作成功，
         *    出让人investor001账户余额增加0.02
         *    受让人investor002冻结金额减少0.03
         *    营销款SYS_GENERATE_002账户余额减少0.01
         *
         *
         */
        eq resp.code, 0
        eq new BigDecimal(investorInfo01A.balance.toString()).subtract(new BigDecimal(investorInfo01B.balance.toString())), amount - 0.02
        eq new BigDecimal(investorInfo02B.freezeAmount.toString()).subtract(new BigDecimal(investorInfo02A.freezeAmount.toString())), amount
        eq new BigDecimal(sys02B.balance.toString()).subtract(new BigDecimal(sys02A.balance.toString())), 0.01
        eq new BigDecimal(incomeUserA.balance.toString()).subtract(new BigDecimal(incomeUserB.balance.toString())), 0.02

    }

    def '代偿预处理'() {

        /**
         * 前置:
         * 1. 存在代偿人guaranteecorp001, 且帐户可用余额充足
         * 2. 变更标的状态为：还款中
         */
        def userInfo1 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: guaranteecorp001
        ])
        direct("MODIFY_PROJECT", [
                requestNo: uuid(),
                projectNo: projectNo,
                status   : 'REPAYING'
        ])

        /**
         * 操作:
         *  发送报文，代偿金额0.06
         *
         * 期望结果:
         * 1. 跳转到输入交易密码页面
         */
        when:
        amount = saleShare + 0.01
        requestNO = uuid()
        def req = [
                platformUserNo: guaranteecorp001,
                requestNo     : requestNO,
                bizType       : "COMPENSATORY",
                amount        : amount,
                projectNo     : projectNo,
                expired       : "20281210120000"
        ]
        gateway('USER_PRE_TRANSACTION', req, "MOBILE")
        then:
        $("#password").size() == 1

        /**
         * 操作:
         *  输入交易密码，点击确定
         *
         * 期望结果:
         * 1. 操作成功，冻结资金增加0.04
         */
        when:
        log "输入密码并点击按钮"
        sleep(2000)
        $("#password").val(password)
        $('#nextButton').click()
        sleep(2000)
        def resp = response()
        def userInfo2 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: guaranteecorp001
        ])

        then:
        eq resp.code, 0
        eq new BigDecimal(userInfo2.freezeAmount.toString()).subtract(new BigDecimal(userInfo1.freezeAmount.toString())), amount
    }

    def '直接代偿确认'() {

        /**
         * 前置:
         * 1. 存在代偿人guaranteecorp001, 手持债权人investor002， 派息还款接收方SYS_GENERATE_002, 分润接收方invester001
         */
        def guaranInfoB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: guaranteecorp001
        ])
        def investorInfo01B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])
        def investorInfo02B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])
        def sys02B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: "SYS_GENERATE_002"
        ])
        def incomeUserB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])


        /**
         * 操作:
         *  发送报文， 代偿金额amount-0.04(0.06-0.04),还派息款SYS_GENERATE_002 0.01 分润invester001 0.01
         *           佣金：代偿人、投资人0.01
         */
        when:
        def requestNo = uuid()
        def req = [
                "batchNo": "cgt_batchno150234r5880902", "bizDetails": [
                [
                        requestNo: requestNo,
                        projectNo: projectNo,
                        tradeType: "COMPENSATORY",
                        details  : [
                                [
                                        bizType             : "COMPENSATORY",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: guaranteecorp001,
                                        targetPlatformUserNo: investor002,
                                        amount              : amount - 0.04,
                                        income              : 0.01,
                                        customDefine        : "2016102100001",
                                        share               : "1",
                                ],
                                [
                                        bizType             : "INTEREST_REPAYMENT",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: guaranteecorp001,
                                        targetPlatformUserNo: "SYS_GENERATE_002",
                                        amount              : "0.01",
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1"
                                ],
                                [
                                        "bizType"             : "PROFIT",
                                        "freezeRequestNo"     : requestNO,
                                        "sourcePlatformUserNo": guaranteecorp001,
                                        "targetPlatformUserNo": investor001,
                                        "amount"              : "0.01",
                                        "customDefine"        : "2016102100001",
                                        income: '1',
                                        share               : "1"
                                ],
                                [
                                        bizType             : "COMMISSION",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: guaranteecorp001,
                                        targetPlatformUserNo: incomeUser,
                                        amount              : "0.01",
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1"
                                ],
                                [
                                        bizType             : "COMMISSION",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: investor002,
                                        targetPlatformUserNo: incomeUser,
                                        amount              : "0.01",
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1"
                                ]
                        ]

                ]
        ]
        ]

        def resp = direct('ASYNC_TRANSACTION', req)

        for (int i = 0; i < 100; i++) {
            def result = direct("QUERY_TRANSACTION", [
                    requestNo      : requestNo,
                    transactionType: "TRANSACTION",
            ])
            if (result.records.status[0].equals("SUCCESS")) {
                println("订单状态成功")
                break;
            }
            sleep(2000)
        }

        then:

        def guaranInfoA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: guaranteecorp001
        ])
        def investorInfo01A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])
        def investorInfo02A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])
        def sys02A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: "SYS_GENERATE_002"
        ])
        def incomeUserA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])


        /**
         * 期望结果:
         * 1. 操作成功，
         *    手持债权人investor002账户余额增加0.01
         *    代偿人guaranteecorp001账户冻结资金减少0.05(amount - 0.01)
         *    代偿人guaranteecorp001账户资金减少0.05(amount - 0.01)
         *    分润方investor001账户余额增加0.01
         *    营销款SYS_GENERATE_002账户余额增加0.01
         *
         *
         */
        eq resp.code, 0
        eq new BigDecimal(investorInfo02A.balance.toString()).subtract(new BigDecimal(investorInfo02B.balance.toString())), 0.01
        eq new BigDecimal(guaranInfoB.freezeAmount.toString()).subtract(new BigDecimal(guaranInfoA.freezeAmount.toString())), amount - 0.01
        eq new BigDecimal(guaranInfoB.balance.toString()).subtract(new BigDecimal(guaranInfoA.balance.toString())), amount - 0.01
        eq new BigDecimal(investorInfo01A.balance.toString()).subtract(new BigDecimal(investorInfo01B.balance.toString())), 0.01
        eq new BigDecimal(sys02A.balance.toString()).subtract(new BigDecimal(sys02B.balance.toString())), 0.01
        eq new BigDecimal(incomeUserA.balance.toString()).subtract(new BigDecimal(incomeUserB.balance.toString())), 0.02

    }

    def '间接代偿确认'() {

        /**
         * 前置:
         * 1. 存在代偿人guaranteecorp001, 手持债权人investor002,借款人borrower001
         */
        def guaranInfoB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: guaranteecorp001
        ])
        def investorInfo02B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])
        def incomeUserB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])


        /**
         * 操作:
         *  发送报文， 代偿金额0.01
         */
        when:
        amount = 0.01
        def requestNo = uuid()
        def req = [
                "batchNo": "cgt_batchno150234r5880902", "bizDetails": [
                [
                        requestNo: requestNo,
                        projectNo: projectNo,
                        tradeType: "INDIRECT_COMPENSATORY",
                        details  : [
                                [
                                        bizType             : "COMPENSATORY",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: guaranteecorp001,
                                        targetPlatformUserNo: borrower001,
                                        amount              : 0.01,
                                        customDefine        : "2016102100001",
                                        share               : "1"
                                ],
                                [
                                        bizType             : "COMPENSATORY",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: borrower001,
                                        targetPlatformUserNo: investor002,
                                        amount              : 0.01,
                                        customDefine        : "2016102100001",
                                        share               : "1"
                                ]
                        ]

                ]
        ]
        ]

        def resp = direct('ASYNC_TRANSACTION', req)

        for (int i = 0; i < 100; i++) {
            def result = direct("QUERY_TRANSACTION", [
                    requestNo      : requestNo,
                    transactionType: "TRANSACTION",
            ])
            if (result.records.status[0].equals("SUCCESS")) {
                println("订单状态成功")
                break;
            }
            sleep(2000)
        }

        then:

        def guaranInfoA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: guaranteecorp001
        ])
        def investorInfo02A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])
        def incomeUserA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])


        /**
         * 期望结果:
         * 1. 操作成功，
         *    手持债权人investor002账户余额增加0.01
         *    代偿人guaranteecorp001账户冻结资金减少0.01
         *
         */
        eq resp.code, 0
        eq new BigDecimal(investorInfo02A.balance.toString()).subtract(new BigDecimal(investorInfo02B.balance.toString())), 0.01
        eq new BigDecimal(guaranInfoB.freezeAmount.toString()).subtract(new BigDecimal(guaranInfoA.freezeAmount.toString())), 0.01
        eq new BigDecimal(incomeUserA.balance.toString()).subtract(new BigDecimal(incomeUserB.balance.toString())), 0
    }

    def '代偿还款预处理'() {

        /**
         * 前置:
         * 1. 存在借款人borrower001, 且帐户可用余额充足
         */
        def userInfo1 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])

        /**
         * 操作:
         *  发送报文，冻结借款人账户资金0.02
         *
         * 期望结果:
         * 1. 跳转到输入交易密码页面
         */
        when:
        amount = 0.02
        requestNO = uuid()
        def req = [
                platformUserNo: borrower001,
                requestNo     : requestNO,
                bizType       : "REPAYMENT",
                amount        : amount,
                projectNo     : projectNo,
                expired       : "20281210120000"
        ]
        gateway('USER_PRE_TRANSACTION', req)

        then:
        $("#password").size() == 1

        /**
         * 操作:
         *  输入交易密码，点击确定
         *
         * 期望结果:
         * 1. 操作成功，借款人冻结资金增加0.02
         */
        when:
        log "输入密码并点击按钮"
        $("#password").val(password)
        $('#nextButton').click()
        sleep(5000)
        def resp = response()
        def userInfo2 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])
        sleep(2000)

        then:
        eq resp.code, 0
        eq new BigDecimal(userInfo2.freezeAmount.toString()).subtract(new BigDecimal(userInfo1.freezeAmount.toString())), amount
    }

    def '代偿还款确认'() {

        /**
         * 前置:
         * 1. 存在代偿人guaranteecorp001,借款人borrower001
         */
        def guaranInfoB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: guaranteecorp001
        ])
        def borrowerInfoB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])
        def incomeUserB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])


        /**
         * 操作:
         *  发送报文，代偿金额0.01,分别收取借款人borrower001、受让方investor001 佣金0.01
         */
        when:
        def requestNo = uuid()
        def req = [
                "batchNo": "cgt_batchno150234r5880902", "bizDetails": [
                [
                        requestNo: requestNo,
                        projectNo: projectNo,
                        tradeType: "REPAYMENT",
                        details  : [
                                [
                                        bizType             : "COMPENSATORY_REPAYMENT",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: borrower001,
                                        targetPlatformUserNo: guaranteecorp001,
                                        amount              : 0.01,
                                        income              : 0.01,
                                        customDefine        : "2016102100001",
                                        share               : "1"
                                ],
                                [
                                        bizType             : "COMMISSION",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: borrower001,
                                        targetPlatformUserNo: incomeUser,
                                        amount              : 0.01,
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1"
                                ]

                        ]

                ]
        ]
        ]

        def resp = direct('ASYNC_TRANSACTION', req)

        for (int i = 0; i < 100; i++) {
            def result = direct("QUERY_TRANSACTION", [
                    requestNo      : requestNo,
                    transactionType: "TRANSACTION",
            ])
            if (result.records.status[0].equals("SUCCESS")) {
                println("订单状态成功")
                break;
            }
            sleep(2000)
        }

        then:

        def guaranInfoA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: guaranteecorp001
        ])
        def borrowerInfoA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])
        def incomeUserA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])


        /**
         * 期望结果:
         * 1. 操作成功，
         *    受让人guaranteecorp001账户余额增加0.01
         *    借款人borrower001账户冻结资金减少0.02
         *
         */
        eq resp.code, 0
        eq new BigDecimal(guaranInfoA.balance.toString()).subtract(new BigDecimal(guaranInfoB.balance.toString())), 0.01
        eq new BigDecimal(borrowerInfoB.freezeAmount.toString()).subtract(new BigDecimal(borrowerInfoA.freezeAmount.toString())), amount
        eq new BigDecimal(incomeUserA.balance.toString()).subtract(new BigDecimal(incomeUserB.balance.toString())), 0.01
    }

    def '代偿还款收罚息预处理'() {
        /**
         * 前置:
         * 1. 存在借款人borrower001, 且帐户可用余额充足
         */
        def userInfo1 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])

        /**
         * 操作:
         *  发送报文，冻结借款人账户资金0.04
         *
         * 期望结果:
         * 1. 跳转到输入交易密码页面
         */
        when:
        amount = 0.04
        requestNO = uuid()
        def req = [
                platformUserNo: borrower001,
                requestNo     : requestNO,
                bizType       : "COMPENSATORY_REPAYMENT",
                amount        : amount,
                projectNo     : projectNo,
                expired       : "20281210120000"
        ]
        gateway('USER_PRE_TRANSACTION', req, "MOBILE")

        then:
        $("#password").size() == 1

        /**
         * 操作:
         *  输入交易密码，点击确定
         *
         * 期望结果:
         * 1. 操作成功，借款人冻结资金增加0.04
         */
        when:
        log "输入密码并点击按钮"
        $("#password").val(password)
        $('#nextButton').click()
        def resp = response()
        def userInfo2 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])

        then:
        eq resp.code, 0
        eq new BigDecimal(userInfo2.freezeAmount.toString()).subtract(new BigDecimal(userInfo1.freezeAmount.toString())), amount
    }

    def '代偿还款收罚息确认'() {

        /**
         * 前置:
         * 1. 存在代偿人guaranteecorp001,借款人borrower001,分润方investor001
         */
        def guaranInfoB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: guaranteecorp001
        ])
        def borrowerInfoB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])
        def investorInfo01B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])
        def incomeUserB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])


        /**
         * 操作:
         *  发送报文，代偿金额0.02, 分润0.01, 收取借款人borrower001佣金0.01
         */
        when:
        def requestNo = uuid()
        def req = [
                "batchNo": "cgt_batchno150234r5880902", "bizDetails": [
                [
                        requestNo: requestNo,
                        projectNo: projectNo,
                        tradeType: "COMPENSATORY_REPAYMENT",
                        details  : [
                                [
                                        bizType             : "COMPENSATORY_REPAYMENT",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: borrower001,
                                        targetPlatformUserNo: guaranteecorp001,
                                        amount              : amount - 0.02,
                                        income              : 0.01,
                                        customDefine        : "2016102100001",
                                        share               : "1"

                                ],
                                [
                                        bizType             : "PROFIT",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: borrower001,
                                        targetPlatformUserNo: investor001,
                                        amount              : 0.01,
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1"
                                ],
                                [
                                        bizType             : "COMMISSION",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: borrower001,
                                        targetPlatformUserNo: incomeUser,
                                        amount              : 0.01,
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1"
                                ]
                        ]

                ]
        ]
        ]

        def resp = direct('ASYNC_TRANSACTION', req)

        for (int i = 0; i < 100; i++) {
            def result = direct("QUERY_TRANSACTION", [
                    requestNo      : requestNo,
                    transactionType: "TRANSACTION",
            ])
            if (result.records.status[0].equals("SUCCESS")) {
                println("订单状态成功")
                break;
            }
            sleep(2000)
        }

        then:

        def guaranInfoA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: guaranteecorp001
        ])
        def borrowerInfoA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])
        def investorInfo01A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])
        def incomeUserA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])


        /**
         * 期望结果:
         * 1. 操作成功，
         *    受让人guaranteecorp001账户余额增加0.02
         *    借款人borrower001账户冻结资金减少0.04
         *    分润方investor001账户余额增加0.01
         *
         */
        eq resp.code, 0
        eq new BigDecimal(guaranInfoA.balance.toString()).subtract(new BigDecimal(guaranInfoB.balance.toString())), 0.02
        eq new BigDecimal(borrowerInfoB.freezeAmount.toString()).subtract(new BigDecimal(borrowerInfoA.freezeAmount.toString())), amount
        eq new BigDecimal(investorInfo01A.balance.toString()).subtract(new BigDecimal(investorInfo01B.balance.toString())), 0.01
        eq new BigDecimal(incomeUserA.balance.toString()).subtract(new BigDecimal(incomeUserB.balance.toString())), 0.01
    }

    def '还款预处理'() {

        /**
         * 前置:
         * 1. 存在借款人borrower001, 且帐户可用余额充足
         */
        def userInfo1 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])

        /**
         * 操作:
         *  发送报文，冻结借款人账户资金0.06
         *
         * 期望结果:
         * 1. 跳转到输入交易密码页面
         */
        when:
        amount = 0.07
        requestNO = uuid()
        def req = [
                platformUserNo: borrower001,
                requestNo     : requestNO,
                bizType       : "REPAYMENT",
                amount        : amount,
                projectNo     : projectNo,
                expired       : "20281210120000"
        ]
        gateway('USER_PRE_TRANSACTION', req, "MOBILE")

        then:
        $("#password").size() == 1

        /**
         * 操作:
         *  输入交易密码，点击确定
         *
         * 期望结果:
         * 1. 操作成功，借款人冻结资金增加0.06
         */
        when:
        log "输入密码并点击按钮"
        $("#password").val(password)
        $('#nextButton').click()
        def resp = response()
        def userInfo2 = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])

        then:
        eq resp.code, 0
        eq new BigDecimal(userInfo2.freezeAmount.toString()).subtract(new BigDecimal(userInfo1.freezeAmount.toString())), amount
    }

    def '还款确认并追加冻结'() {

        /**
         * 前置:
         * 1. 借款人borrower001, 投资人investor002,派息账户SYS_GENERATE_005, 分润方investor001
         */
        def borrowerInfoB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])
        def investorInfo01B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])
        def investorInfo02B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])
        def sys05B = direct("QUERY_USER_INFORMATION", [
                platformUserNo: "SYS_GENERATE_005"
        ])
        def incomeUserB = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])


        /**
         * 操作:
         *  发送报文，还款金额0.04, 分别收取借款人borrower001、投资人investor002 佣金0.01、分润0.01
         *           还派息款到SYS_GENERATE_005账户0.01
         */
        when:
        def requestNo = uuid()
        sleep(1000)
        def req = [
                "batchNo": "cgt_batchno150234r5880902", "bizDetails": [
                [
                        requestNo: requestNo,
                        projectNo: projectNo,
                        tradeType: "REPAYMENT",
                        details  : [
                                [
                                        bizType             : "REPAYMENT",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: borrower001,
                                        targetPlatformUserNo: investor002,
                                        amount              : amount - 0.03,
                                        income              : 0.01,
                                        customDefine        : "2016102100001",
                                        share               : "1"
                                ],
                                [
                                        bizType             : "INTEREST_REPAYMENT",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: borrower001,
                                        targetPlatformUserNo: "SYS_GENERATE_005",
                                        amount              : 0.01,
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1"
                                ],
                                [
                                        bizType             : "PROFIT",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: borrower001,
                                        targetPlatformUserNo: investor001,
                                        amount              : 0.01,
                                        income              : 0.01,
                                        customDefine        : "2016102100001",
                                        share               : "1"
                                ],
                                [
                                        bizType             : "PROFIT",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: investor002,
                                        targetPlatformUserNo: investor001,
                                        amount              : 0.01,
                                        income              : 0.01,
                                        customDefine        : "2016102100001",
                                        share               : "1"
                                ],
                                [
                                        bizType             : "COMMISSION",
                                        freezeRequestNo     : requestNO,
                                        sourcePlatformUserNo: borrower001,
                                        targetPlatformUserNo: incomeUser,
                                        amount              : 0.01,
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1"
                                ],
                                [
                                        bizType             : "COMMISSION",
                                        sourcePlatformUserNo: investor002,
                                        targetPlatformUserNo: incomeUser,
                                        amount              : 0.01,
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1"
                                ],
                                [
                                        bizType             : "APPEND_FREEZE",
                                        freezeRequestNo     : freezeRequestNO,
                                        sourcePlatformUserNo: investor002,
                                        amount              : 0.02,
                                        customDefine        : "2016102100001",
                                        income: '1',
                                        share               : "1",

                                ]


                        ]

                ]
        ]
        ]

        def resp = direct('ASYNC_TRANSACTION', req)

        for (int i = 0; i < 100; i++) {
            def result = direct("QUERY_TRANSACTION", [
                    requestNo      : requestNo,
                    transactionType: "TRANSACTION",
            ])
            if (result.records.status[0].equals("SUCCESS")) {
                println("订单状态成功")
                break;
            }
            sleep(2000)
        }

        then:

        def borrowerInfoA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: borrower001
        ])
        def investorInfo01A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor001
        ])
        def investorInfo02A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: investor002
        ])
        def sys05A = direct("QUERY_USER_INFORMATION", [
                platformUserNo: "SYS_GENERATE_005"
        ])
        def incomeUserA = direct("QUERY_USER_INFORMATION", [
                platformUserNo: incomeUser
        ])


        /**
         * 期望结果:
         * 1. 操作成功，
         *    借款人borrower001账户冻结资金减少0.07
         *    投资人账户余额增加0.02且账户冻结资金增加0.02
         *    分润方investor001账户余额增加0.01
         *    派息账户增加余额0.01
         */
        eq resp.code, 0
        eq new BigDecimal(borrowerInfoB.freezeAmount.toString()).subtract(new BigDecimal(borrowerInfoA.freezeAmount.toString())), amount
        eq new BigDecimal(investorInfo02A.balance.toString()).subtract(new BigDecimal(investorInfo02B.balance.toString())), 0.02
        eq new BigDecimal(investorInfo02A.freezeAmount.toString()).subtract(new BigDecimal(investorInfo02B.freezeAmount.toString())), 0.02
        eq new BigDecimal(investorInfo01A.balance.toString()).subtract(new BigDecimal(investorInfo01B.balance.toString())), 0.02
        eq new BigDecimal(sys05A.balance.toString()).subtract(new BigDecimal(sys05B.balance.toString())), 0.01
        eq new BigDecimal(incomeUserA.balance.toString()).subtract(new BigDecimal(incomeUserB.balance.toString())), 0.02
    }


}

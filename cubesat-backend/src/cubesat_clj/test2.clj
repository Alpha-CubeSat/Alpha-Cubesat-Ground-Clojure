(ns cubesat-clj.test2
  (:require [cubesat-clj.telemetry.cubesat-telemetry :as cs])
  (:gen-class))

(def a1 "2A0000000000FFD8FFFE0024F800EB250000000000000000000000000000007800A0001E0032120B510451040000FFDB00840008050607060508070607080808090B130C0B0A")
(def a2 "2A00000000010A0B1710110E131B181D1C1B181A1A1E222B251E2029201A1A263326292D2E3031301D243539352F382B2F302E010808080B0A0B160C0C162E1F1A1F2E2E2E2E")
(def a3 "2A00000000022E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2E2EFFFE0005000000FFC0001108007800A00301")
(def a4 "2A00000000032100021101031101FFC401A20000010501010101010100000000000000000102030405060708090A0B0100030101010101010101010000000000000102030405")
(def a5 "2A0000000004060708090A0B100002010303020403050504040000017D01020300041105122131410613516107227114328191A1082342B1C11552D1F02433627282090A1617")
(def a6 "2A000000000518191A25262728292A3435363738393A434445464748494A535455565758595A636465666768696A737475767778797A838485868788898A9293949596979899")
(def a7 "2A00000000069AA2A3A4A5A6A7A8A9AAB2B3B4B5B6B7B8B9BAC2C3C4C5C6C7C8C9CAD2D3D4D5D6D7D8D9DAE1E2E3E4E5E6E7E8E9EAF1F2F3F4F5F6F7F8F9FA11000201020404")
(def a8 "2A000000000703040705040400010277000102031104052131061241510761711322328108144291A1B1C109233352F0156272D10A162434E125F11718191A262728292A3536")
(def a9 "2A00000000083738393A434445464748494A535455565758595A636465666768696A737475767778797A82838485868788898A92939495969798999AA2A3A4A5A6A7A8A9AAB2")
(def a10 "2A0000000009B3B4B5B6B7B8B9BAC2C3C4C5C6C7C8C9CAD2D3D4D5D6D7D8D9DAE2E3E4E5E6E7E8E9EAF2F3F4F5F6F7F8F9FAFFDD0004000AFFDA000C03010002110311003F00")
(def a11 "2A000000000AEF6D23F2E3C7B554BD5DF2F5E9EF5BB5EED8E54636B7A79B8846074F5AE72D7469127DC32306B83138773699BC64AC74BA78F2500073DB356A6B8F97AE3EB5DF")
(def a12 "2A000000000B15CB1317B981AC7EF908CF26B961A6C8D3E70C0679F715E76269B949336A6ED1773A8D26010C6ABD4E315AC1B0B806BBA92B46C672D5991AAC624539EB5C3DF6")
(def a13 "2A000000000C9B289DCA6E01893C0EB5C38C85DDCD694ADA17B4B81EDD325C939F5E7F956CC3236C3B8E7F1A54172C7714F72BDEA79D1328241C60115C66A166439DC491D064")
(def a14 "2A000000000D572D697BF735A7D8BDA65B98E363B88DA393D3B559B4389F6BB1233D79E9FE49FE54A97C6AE2A8DBD916B54FDF45F31C295EA467F1AF3BD56C584A6455386E7A")
(def a15 "2A000000000EE719ED5DDCD69EBD4CA3B1FFD0F4195963523354D7F792FB574CB576395124F00615525B40A38FE544A3708B29C89B3355266383CD4B1DCA32292DCD462DC83C")
(def a16 "2A000000000F8FC45636BB2FA17604D8063152B1C1C67AD6B15642653B85DCC6B2EE6D8313C0C77E2B1A8AE545D883C8DA33C0239A6336D38E00ED585AC31508707918C76ACB")
(def a17 "2A0000000010B98184872547D7A1FF003CD715785DEA6B176D50D8A12176E4631D00E2A94CDE5CE470A41C7CD8E68845DEE127765A69BED0983CB3718FF3DAB26F6D4062FD40")
(def a18 "2A0000000011FBA41EA38FFEB71ED5ACECF5222ADA1FFFD1EAA2BCFB511B4F15A36F18400E6B783E6399AB6848C7E6F6A867E87E95A325332AE38354251F31E462B39772D113")
(def a19 "2A000000001201C66957039FE75161B24CE31D3E94C99B8EBF8555C4566626A071BF3D31594CA4529CE0E3354267E831DBB8AC66CB4245261C0560491CD2DC60E082037527B5")
(def a20 "2A000000001371D57ADCD117349D267D4E3FF461929D7FA573FAFD94904CF13AED917E5604F03D2B38CD5EC81C5A49988D78D03E2470A06071DBF0AAF73ABA48572D8C75C9FF")
(def a21 "2A0000000014001AA519315B53FFD2E83445DB1A935B5BCF6AD68FC28E796E01B9273513B120F35AB6494271D6A9C89CE6A58D10B802A1DC41C566F42D0A588EF513CB9EA7F5")
(def a22 "2A0000000015A4E409159A4EB8C63D6A269FD78AC66F42D2285C499739359F23E791F4FBDD6B9AA4D24528EA470C84C838C63A0AB523029C60641E2B8AA4EEECB7344BB12687")
(def a23 "2A0000000016AFDDE8B3C9E40044CA50AE7A9EC47E354F5F9669A57B8B8DA59CF38278ACE0F95DCA7B58E2F5BF98B32E071C60E6B9C77607058F5CF5AF4F0D671226AC7FFFD3")
(def a24 "2A0000000017E9AD2331803B0AB5E673915B46F639D81738EB48CDF21AB422B499FC2A0907B52680A93838C2FE75498B03D075ACA772D0A49DBD7B5549198B1F4ACE7729103B")
(def a25 "2A000000001814FCBAD53B876C86E801E4D73D57A1A45220BA7664C8E33D4F6AC99E523D4907AF5AE2A97348D931124C3F3BB23F1AB6F3931705738C7FFAAB9AA21AD59952DC")
(def a26 "2A00000000190CB00B9F4C1FAD56BDD4AE240118EE1CE7241FC78AA84399EA0EDD0C1BC94B8638C927A7240FF3FE3591329DD92319AF5282B2339ED63FFFD4EBB6851DE99924")
(def a27 "2A000000001AF7ADDEC736E21CE697394FD28404449150C809A6C0AF30C8AAB247D73CD6522D10CA368E38AA12B807B815949968AD3B8C6719CFE95426932580071FCEB92B3D")
(def a28 "2A000000001B19A448D240D09DDD7A609ACAB9708C783E80035E7CA5D0DB95B2AB3FCA30DD4E73E9FAD4BE78309DD9CE3DB3FCEA27AEA38A33669BE76DA49C70324F07FCFF00")
(def a29 "2A000000001C3AA7712654939391D335D14D6C16E866CC4ED391C678355E51F2F072A4FB577C343391FFD5EB239778E294E4738ADB7D8E72119278E314E6E10035490880B107")
(def a30 "2A000000001D9A6B124679A24BB022AC848AAB2480641E4D6323445795F83FA5509DC721B9AC66D96919F23E33B4E05673DC85620F73F9D70D59B348A205B860DB58FDE3C554")
(def a31 "2A000000001EBBDDBBE50724D79EDEA6F6B148BB023AF4ED4D0F9424676F3CE7E95ADB41ADC84B96605B18CD40E58920F208E3FCF6AD621668A72F249C0EBDC554919429C839")
(def a32 "2A000000001FC63FFAF5D90224ADA9FFD6DFD2A5F36356041C8AB97126C8F35AC5DE273BDEC5482E959F04D59760573C5541DC522238EF50B38C554AE0569DFA9159F3360E6B")
(def a33 "2A000000002009968AF23ED53CD655DCADBB183915CB565646C8A334879C1C9038AC9BA2739008E4F207F9F6AE1A92B9A4515E3903B9C9248E739EBED4EBA9370041EDE9EB5C")
(def a34 "2A0000000021B3DCDD6A529DBA30DF80D9191D4D41BB200E3DC807FAFD6B68AD0860598A8E33D7D6A0739C8279C74AD2256A8A173285CED39E3D3159D2485DB249AEFA51D2E6")
(def a35 "2A0000000022151B3FFFD7D5D1B74400604002B4AF54C917CB5A417B9639DEE73EA92C771CE47E15B303B18D41FD69D34D3D425A8AFED503E4FAD6921245791B8AA939C77AE7")
(def a36 "2A00000000239968A13498159572C724E7271E95C959D91AC4A13B01D803E959D724B641C1FA77AF3E46F129C87610EB8CFB71CD30CE59739208159B8DF5348B2ABC9C9E7069")
(def a37 "2A0000000024B8CF5C1E7A66B6D902239E5DAD8C671ED9AA52DC6E046723D8D6D4E17D4536BA14279198924F19E2A0ED5DF156473C99FFD0E8CDB843B978C5489203F2B574EA")
(def a38 "2A00000000259D8E41248A327951834D7DB1E369EDEB54DEA1EA46D255791FB8A96CA4549DFB8C71542E26033DEB9E6CD114279B39009CD674F211DC7A1AE3AB2B2358A33EE6")
(def a39 "2A000000002641D0F071922B3E6936B64F23BE48CD79EDF6358A29B39E41507DB3D4D570EC198E4E3D3B55248BB6A30C8A48DF91DFAF151CF7380CABD0FA569185D8D6851925")
(def a40 "2A000000002725C82E3D3FC2AA48FDFF00035D908D8CA4472123383D4FAE6A33D6B789933FFFD1EAA37DEB9C9231D6A95D49E5C9FF00D6AE893D2E72A229EF551324FD2A8CBA")
(def a41 "2A0000000028A213C367DF1594EA24CA51B924777BF807F1C53D9F233FD2AAE066DF5C841D4FE558375AA286393C77E2B8ABD551D0DA312ABDE060083D3AE4545E66F181935C")
(def a42 "2A00000000296E5CC8D52D0A770E17A0381DB15933CB8EE49C6391585AECD12D08198B0E096F4C8E9FE4D318EDE7DBFBB5AA486DEA569A53DF771CE2AA4F30DC464F5CFDDC66")
(def a43 "2A000000002ABA29C49BA2BB396233D7B71516FE4FD7B574A48CE4D8D24FBE7A537233EB5A22247FFFD2DDD32E04910CFA7AD53D5A4C4C02FF003AD1CAF13996E616B134AB07")
(def a44 "2A000000002BC9D4FA9EF5C9CFA838B904E4639E735E562E72F69A1D34D2E53A8D26EB7226E6078C939AD7F33299E3A57A14DDE2998C95B43035D76F25B18E2B8B9E770C4970")
(def a45 "2A000000002CA7A75FEB5E6E275A874D24AC5AB59F2B8C8E9939CD5D89C1FEE9E723B56114F545CB7295FCA14632AA00C83CFD3FCFD2B02793E6E08209E7F5ABA4AED8D2D058")
(def a46 "2A000000002DE42F9FBBEBC9E94162D9385C9F735AD95C6CAD3601C285CF4E2A94C79FF03D2BA29912D8889C1EDEF9A60FC2BA1193131F4A4AA4433FFFD3BDA5B9440ADBB814")
(def a47 "2A000000002E5D8F3261C13571BF2599CF7D6E57BAB4F3931B720F1CD73B7FA2AF98CE2360C78CE07AD72D7A3CC5C25624B385A00A141017B56AC326E5032735A52565A8A4D1")
(def a48 "2A000000002F4F52804A8460FE5C572F77A49DFB829F4AE3C4D36DDCDA9C8805A344C382075FF3CD5B5DA14E4367BE7BD7226E3B9A369AD0CDBF1927861918209EBE9590F6E7")
(def a49 "2A00000000308CAE4FA647E1554DD8B5B08220AB92A73DF9EF4D3851C8C0E319ED5ADEE2B762A4FBBA9DC323B71558A6EE4020FD2BA60F413D771AF1F1C753CE6A1298C704E7")
(def a50 "2A00000000319CFB56B1666D6A23293F74647D6811311D39ABE6489E5B9FFFD4D29A358BEEE306AB871E6727A9F435A349339EE5D054A0C7EB54EE634391C53924233E681539")
(def a51 "2A0000000032503F2A84481181518E79CD66D24574257652BF36DFAD519D23392403595449E8CA4EE50BA8D00C803F5AC9970921418EB915E7D54AFA1B4595A408EE4B04C8E4")
(def a52 "2A000000003375355E60801190063F3AC2DA97A959955890029507A9C8FF003FFD6AA93F9647200047BF4ADA3B94D94E41B8103A671F37514CDAA381B73F439F7AE94F4B0AD7")
(def a53 "2A00000000341DE5163D300F1900D466D1727186C0EB834D54B072DDEA491C11819032DDC63FCF6A0850030007B0ED4B99B1B4B63FFFD5B37573BB0320E7FDAAA666C3E49F7F")
(def a54 "2A0000000035BDD2AA52D4E74AC89CDEEC51B8918F7ACE9B5B84CDB3CC5CFF00BE2A255527661CBA683CDD871D41FF00815539E50A548E067D69CA5A5C488AF2F04716E66EDE")
(def a55 "2A0000000036B58EFAD209304E403CF238AE7AD51AD8D20879BF0F1E4370472460E2A83CCAF31CB7E6D8AE1AAEE91B27DC81A53E61C7393CFCDFD6A94D76ABC33648E393D6A5")
(def a56 "2A00000000372BBB22ED7640660E38C8EC47A535D1D8901CF03190D5A6C36B5D069B628410493DF6B60F7A8A48C2EEC91919046EC60E6A94EE55AEB51F1BC6A842E73FEF7FF5")
(def a57 "2A0000000038AA26947F7B3EBCF1FE79AA5177B8D5885A6C02776403D73FE7DEA0964CE0924E3B66B684452763FFFFFFFFD9BFBFBFBFBFBFBFBFBFBFBFBFBFBFBFBFBFBFBFBF")

(defn -main []
  (do (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a1]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a2]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a3]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a4]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a5]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a6]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a7]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a8]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a9]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a10]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a11]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a12]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a13]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a14]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a15]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a16]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a17]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a18]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a19]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a20]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a21]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a22]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a23]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a24]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a25]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a26]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a27]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a28]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a29]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a30]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a31]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a32]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a33]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a34]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a35]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a36]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a37]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a38]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a39]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a40]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a41]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a42]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a43]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a44]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a45]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a46]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a47]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a48]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a49]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a50]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a51]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a52]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a53]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a54]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a55]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a56]))
      (cs/save-ttl-data (cs/read-packet-data [::cs/ttl a57]))
      (println "Done")))
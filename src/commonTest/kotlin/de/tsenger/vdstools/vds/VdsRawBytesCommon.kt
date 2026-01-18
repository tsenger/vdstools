package de.tsenger.vdstools.vds

@OptIn(ExperimentalStdlibApi::class)
object VdsRawBytesCommon {


    val residentPermit: ByteArray =
        ("DC03D9C5D9CAC8A73A990F71346ECF47" +
                "FB0602305CBA135875976EC066D417B5" +
                "9E8C6ABC133C133C133C133C3FEF3A29" +
                "38EE43F1593D1AE52DBB26751FE64B7C" +
                "133C136B0306D79519A65306FF408B7F" +
                "3B5F9A83FDD4F46EC7DCCC3384BB6C54" +
                "0AAF52603CC66D1F08B7F5E71243475D" +
                "0A833B51FD2A846622E847B1F3791803" +
                "F26D734B9BD18178FA22CFF2A31A").hexToByteArray()

    var addressStickerId: ByteArray =
        ("DC03D9C56D32C8A72CB16ECF476ECF47" +
                "F9080106CF3519AF974C02061A702085" +
                "19A1030E395E463E740C749FAD19D31E" +
                "FE32FF388F16D14E828A86E0F8E31DDF" +
                "13CC2CB3E0D8F5E4706562C9503D7326" +
                "AD6C0FA84154607D70975E5B7DFB2E36" +
                "197988ECE64345D37AD9B97C").hexToByteArray()


    var socialInsurance: ByteArray =
        ("DC02D9C56D32C8A519FC0F71346F1D67" +
                "FC0401083FEE456D2DE019A8020B5065" +
                "72736368776569C39F03054F73636172" +
                "04134AC3A2636F62C3A96E6964696374" +
                "7572697573FF401DCE81E863B01CFFE5" +
                "B099A5BBFCA60730EC9E090A1C82FA00" +
                "580EB592A9FC921D5F02CE8D1EC4E3AA" +
                "3CB4CEA3AFEF1C382B44ED8DA7105372" +
                "FC1D2E8D91A393").hexToByteArray()


    var arrivalAttestationV02: ByteArray =
        ("DC02D9C56D32C8A51A540F71346F1D67" +
                "FD020230A56213535BD4CAECC87CA4CC" +
                "AEB4133C133C133C133C133C3FEF3A29" +
                "38EE43F1593D1AE52DBB26751FE64B7C" +
                "133C136B030859E9203833736D24FF40" +
                "77B2FEC8EF9EF10C0D38A7D2A579EBB9" +
                "F80212EB06EDD7B1DC29889A6B735B7E" +
                "A1D7D78FF60D2AECB87B0247628C3211" +
                "9BA335B6BD87A7E07333C83ED16B091F").hexToByteArray()


    var arrivalAttestation: ByteArray =
        ("dc026abc6d32c8a519fc0f71341145f4" +
                "fd020230a5621353d9a275735bd4134b" +
                "c549133c133c133c133c133ca32519a5" +
                "19a4344a5e681ae7204b20d532cf4b7c" +
                "133c133f030820d5201019a51aeaff40" +
                "4a1f218ca4392647ecff6c8abf9e796a" +
                "78eebe0b1ac8cc25c4ee17eed961d118" +
                "9091358d7d616f1a517abc747f6c4490" +
                "ff159d4dcf50248b00b1e32e9e7805e7").hexToByteArray()


    var visa_224bitSig: ByteArray =
        ("DC03D9C56D32C8A72CB10F71347D0017" +
                "5D01022CDD52134A74DA1347C6FED95C" +
                "B89F9FCE133C133C133C133C20383373" +
                "4AAF47F0C32F1A1E20EB2625393AFE31" +
                "0403A00000050633BE1FED20C6FF389F" +
                "D029C66FB2E4BF361CDBFFD8F5931B62" +
                "59F645B077702C617F453D0B898A55E6" +
                "E7870974FFE7B3AC416ACDE6B03B3C3A" +
                "8CB5A22B456816").hexToByteArray()


    var supplementSheet: ByteArray =
        ("DC03D9C5D9CAC8A73A990F71347D4E37" +
                "FA0604305CBA135875976EC066D417B5" +
                "9E8C6ABC133C133C133C133C3FEF3A29" +
                "38EE43F1593D1AE52DBB26751FE64B7C" +
                "133C136B0506B77519A519AAFF4008F9" +
                "E9B4B79BE5703048A4879A4F420C433C" +
                "375295A355FB0D29DCBED211CF6F5F57" +
                "38BA2B74E2FE5F1D2D2021E054BFFD0E" +
                "4CE17D98E5BCED26A85C91C68B2F").hexToByteArray()


    var addressStickerPassport: ByteArray =
        ("DC03D9C5D9CAC8A73A990F71347D4E37" +
                "F80A0106B77A38E596CE02061A203A4D" +
                "1FE1030426532081FF4027436CE719F9" +
                "13CCD3EBFAEEAE175171450DB6CA1B62" +
                "FF188748834D2DC5299A5F418BE8D4DC" +
                "052E0536CB6DE711B4CC645651C6B0EA" +
                "FE5713E96290DC149169").hexToByteArray()


    var emergenyTravelDoc: ByteArray =
        ("DC03D9C5D9CAC8A73A990F71347D4E37" +
                "5E0302308A0D62B9D917A4CCA93CA4D0" +
                "EDFC133C133C133C133C133C3FEF3A29" +
                "38EE43F1593D1AE52DBB26751FE64B7C" +
                "133C136BFF4022F8BD19ECCBA4EF24F2" +
                "04787796DD914FEC61F605B153B22A6E" +
                "F307D3869938A4E7E908F0A63B837988" +
                "0B395C7FDBAC720D7F2836D08E1DA626" +
                "11614A00120B").hexToByteArray()


    var permanentResidencePermit: ByteArray =
        ("dc036abcd9cac8a73a99a807baa807ba" +
                "f48f010659e96b0f2d0a0206e95545b8" +
                "19f6ff4062672e113775b885000fa173" +
                "0c6e0eeaa8508cf56f01bccce8bf8e74" +
                "05de9d6544645b5c1e491c9be01a9ad5" +
                "7216703257cd12c678fd3d55d7118670" +
                "1e635364").hexToByteArray()

    val ankunftsnwPapier: ByteArray =
        ("dc03fe456d2b712a19a51ae11eb3704f" +
                "d590fd020230a5621353d9a275735bd4" +
                "134bc54957fc133c133c133c133ca306" +
                "2064339630e7c3591ae626fc20d545de" +
                "327c133c134503082038337346ae19cf" +
                "ff4092803028bcbef5d7b9daf00c67a5" +
                "865b26b38caea2664c722b4b1572424f" +
                "9e4a3d90d80c4d962bf29eb95e09da6a" +
                "de68124052479585d9756353012dee6d" +
                "41b2").hexToByteArray()

    val ankunftsnwPapier2: ByteArray =
        ("dc03fe456d2b712519ad1eb3704f" +
                "d590fd020230a5621353d9a275735bd4" +
                "134bc54957fc133c133c133c133ca306" +
                "2064339630e7c3591ae626fc20d545de" +
                "327c133c134503082038337346ae19cf" +
                "ff4092803028bcbef5d7b9daf00c67a5" +
                "865b26b38caea2664c722b4b1572424f" +
                "9e4a3d90d80c4d962bf29eb95e09da6a" +
                "de68124052479585d9756353012dee6d" +
                "41b2").hexToByteArray()

    val meldebescheinigung: ByteArray =
        ("dc036abc6d38dbdf58c6724b34127770" +
                "34bb3a2f5834724919da47cb59614e76" +
                "094e760901c800109a4223406d374ef9" +
                "9e2cf95e31a23846040a4d7573746572" +
                "6d616e6e050344722e06054572696b61" +
                "0700080a31312e31322e313936340910" +
                "4b6f6d6d616e64616e74656e7374722e" +
                "0a0231380b0531303936390c06426572" +
                "6c696e0d0832303235303431340e0100" +
                "0f083230323530353034ff407b423fba" +
                "c54a00d992faec76b70966dbfb611f67" +
                "da6ff83d3bcf8b64ffede2be2201169d" +
                "e7fb866f048df6e2cd1528767d8d6337" +
                "20465396e64681e3a49ec74d").hexToByteArray()
}



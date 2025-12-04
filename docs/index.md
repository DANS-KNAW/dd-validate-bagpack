dd-validate-bagpack
===================

Checks if a bag conforms to the BagPack specifications and the DANS BagPack profile.

Purpose
-------
This service validates BagPacks according to the BagPack specifications and the DANS BagPack profile to ensure that they can be properly processed by
[dd-transfer-to-vault]{:target=_blank}.

Interfaces
----------

This service has the following interfaces:

![interfaces](./img/overview.png){width="70%"}

### Provided interfaces

#### Command interface

* _Protocol type_: HTTP
* _Internal or external_: **internal**
* _Purpose_: to receive commands to validate BagPacks and report the results

#### BagPacks

* _Protocol type_: Shared filesystem
* _Internal or external_: **internal**
* _Purpose_: to receive BagPacks from other services for validation

#### Admin

* _Protocol type_: HTTP
* _Internal or external_: **internal**
* _Purpose_: application monitoring and management

Processing
----------

The service receives requests for validation via the command interface. Each request contains the path to a BagPack on the shared filesystem. The client can
track the progress of the validation by requesting the status of the validation using the command interface.

A validation task runs a `BagValidatorRule` for each rule defined in the [DANS BagPack profile]{:target=_blank}, provided that the rule is specified as a "
MUST". Rules that are specified as "SHOULD" or "MAY" are not validated. Rules that depend on other rules passing are only validated if those other rules have
passed. When all applicable rules have been validated, a validation result is generated. The result contains the overall validation status (compliant or not),
as well as a list of rule violations.

Tasks can be run in parallel, depending on the configuration of the service, see `validation.taskQueue` in the [configuration source file]{:target=_blank} for
details.

[dd-transfer-to-vault]: {{ transfer_to_vault_url }}
[DANS BagPack profile]: {{ bagpack_profile_url }}
[configuration source file]: {{ config_source_url }}
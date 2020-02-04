package cmd

import (
	"fmt"
	"time"

	"github.com/SAP/jenkins-library/pkg/config"
	"github.com/SAP/jenkins-library/pkg/log"
	"github.com/SAP/jenkins-library/pkg/telemetry"
	"github.com/spf13/cobra"
)

type versionOptions struct {
}

// VersionCommand Returns the version of the piper binary
func VersionCommand() *cobra.Command {
	metadata := versionMetadata()
	var stepConfig versionOptions
	var startTime time.Time

	var createVersionCmd = &cobra.Command{
		Use:   "version",
		Short: "Returns the version of the piper binary",
		Long:  `Writes the commit hash and the tag (if any) to stdout and exits with 0.`,
		PreRunE: func(cmd *cobra.Command, args []string) error {
			startTime = time.Now()
			log.SetStepName("version")
			log.SetVerbose(GeneralConfig.Verbose)
			return PrepareConfig(cmd, &metadata, "version", &stepConfig, config.OpenPiperFile)
		},
		Run: func(cmd *cobra.Command, args []string) {
			telemetryData := telemetry.CustomData{}
			telemetryData.ErrorCode = "1"
			handler := func() {
				telemetryData.Duration = fmt.Sprintf("%v", time.Since(startTime).Milliseconds())
				telemetry.Send(&telemetryData)
			}
			log.DeferExitHandler(handler)
			defer handler()
			telemetry.Initialize(GeneralConfig.NoTelemetry, "version")
			version(stepConfig, &telemetryData)
			telemetryData.ErrorCode = "0"
		},
	}

	addVersionFlags(createVersionCmd, &stepConfig)
	return createVersionCmd
}

func addVersionFlags(cmd *cobra.Command, stepConfig *versionOptions) {

}

// retrieve step metadata
func versionMetadata() config.StepData {
	var theMetaData = config.StepData{
		Spec: config.StepSpec{
			Inputs: config.StepInputs{
				Parameters: []config.StepParameters{},
			},
		},
	}
	return theMetaData
}

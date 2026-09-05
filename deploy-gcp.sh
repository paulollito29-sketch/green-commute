#!/usr/bin/env bash
set -e

echo "🌿 =========================================="
echo "   EcoCommute (ODS 11) - Despliegue en GCP"
echo "=========================================="

PROJECT_ID=$(gcloud config get-value project 2>/dev/null)
if [ -z "$PROJECT_ID" ]; then
    echo "❌ No hay un proyecto de GCP configurado en gcloud."
    echo "Por favor ejecuta: gcloud config set project TU_PROJECT_ID"
    exit 1
fi

SERVICE_NAME="ecocommute-web"
REGION="us-central1"

echo "🚀 Desplegando en Google Cloud Run..."
echo "• Proyecto GCP: $PROJECT_ID"
echo "• Servicio:     $SERVICE_NAME"
echo "• Región:       $REGION"
echo ""

# Deploy from source using Cloud Build & Cloud Run
gcloud run deploy "$SERVICE_NAME" \
    --source . \
    --region "$REGION" \
    --platform managed \
    --allow-unauthenticated \
    --port 8080 \
    --memory 1024Mi \
    --cpu 1 \
    --min-instances 0 \
    --max-instances 5

echo ""
echo "✅ ¡Despliegue completado con éxito en Google Cloud Run!"
gcloud run services describe "$SERVICE_NAME" --region "$REGION" --format 'value(status.url)'
